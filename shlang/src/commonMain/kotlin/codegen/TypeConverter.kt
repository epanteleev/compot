package codegen

import common.assertion
import types.*
import ir.types.*
import ir.value.*
import ir.Definitions.WORD_SIZE
import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.instruction.Alloc
import ir.instruction.IntPredicate
import ir.module.builder.impl.FunctionDataBuilder
import ir.module.builder.impl.ModuleBuilder
import ir.value.constant.*
import typedesc.TypeHolder


object TypeConverter {
    inline fun <reified T : NonTrivialType> ModuleBuilder.toIRLVType(typeHolder: TypeHolder, type: CType): T {
        val converted = toIRType<Type>(typeHolder, type)
        return if (converted == Type.U1) {
            Type.I8 as T
        } else {
            converted as T
        }
    }

    inline fun <reified T : Type> ModuleBuilder.toIRType(typeHolder: TypeHolder, type: CType): T {
        val converted = toIRTypeUnchecked(typeHolder, type)
        if (converted !is T) {
            throw RuntimeException("Cannot convert '$type' to ${T::class}")
        }

        return converted
    }

    fun ModuleBuilder.toIRTypeUnchecked(typeHolder: TypeHolder, type: CType): Type = when (type) {
        BOOL   -> Type.U1
        CHAR   -> Type.I8
        UCHAR  -> Type.U8
        SHORT  -> Type.I16
        USHORT -> Type.U16
        INT    -> Type.I32
        UINT   -> Type.U32
        LONG   -> Type.I64
        ULONG  -> Type.U64
        FLOAT  -> Type.F32
        DOUBLE -> Type.F64
        VOID   -> Type.Void
        is CStructType -> convertStructType(typeHolder, type)
        is CArrayType -> {
            val elementType = toIRType<NonTrivialType>(typeHolder, type.type.cType())
            ArrayType(elementType, type.dimension.toInt())
        }
        is CStringLiteral -> {
            val elementType = toIRType<NonTrivialType>(typeHolder, type.type.cType())
            ArrayType(elementType, type.dimension.toInt())
        }
        is CUnionType -> convertUnionType(typeHolder, type)
        is CPointer -> Type.Ptr
        is CEnumType -> Type.I32
        is CFunctionType, is CUncompletedArrayType, is AbstractCFunction -> Type.Ptr
        else -> throw RuntimeException("Unknown type, type=$type, class=${type::class}")
    }

    private fun ModuleBuilder.convertStructType(typeHolder: TypeHolder, type: AnyCStructType): Type {
        val fields = type.fields().map { toIRLVType<NonTrivialType>(typeHolder, it.cType()) }
        val structType = findStructTypeOrNull(type.name)
        if (structType != null) {
            return structType
        }

        return structType(type.name, fields)
    }

    private fun ModuleBuilder.convertUnionType(typeHolder: TypeHolder, type: CUnionType): Type {
        val field = type.fields().maxByOrNull { it.cType().size() }.let {
            if (it == null) {
                null
            } else {
                toIRType<NonTrivialType>(typeHolder, it.cType())
            }
        }
        val structType = findStructTypeOrNull(type.name)
        if (structType != null) {
            return structType
        }

        return if (field == null) {
            structType(type.name, listOf())
        } else {
            structType(type.name, listOf(field))
        }
    }

    fun FunctionDataBuilder.toIndexType(value: Value): Value {
        if (value.type() == Type.U1) {
            return convertToType(value, Type.I64)
        }

        return convertToType(value, Type.I64)
    }

    fun FunctionDataBuilder.convertRVToType(value: Value, toType: Type, cvtType: IntegerType = Type.I8): Value {
        val rightCvt = convertToType(value, toType)
        return when (toType) {
            Type.U1 -> flag2int(rightCvt, cvtType)
            else    -> rightCvt
        }
    }

    fun FunctionDataBuilder.convertToType(value: Value, toType: Type): Value {
        if (value.type() == toType) {
            return value
        }
        if (value is PrimitiveConstant && toType !is PointerType) {
            // Opt IR does not support pointer constants.
            return convertConstant(value, toType)
        }

        return when (toType) {
            Type.I8 -> {
                toType as SignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I16 -> trunc(value, toType)
                    Type.I32 -> trunc(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8 ->  bitcast(value, toType)
                    Type.U16 -> trunc(value, toType)
                    Type.U32 -> {
                        val trunc = trunc(value, Type.U8)
                        bitcast(trunc, toType)
                    }
                    Type.U64 -> {
                        val bitcast = bitcast(value, Type.I64)
                        trunc(bitcast, toType)
                    }
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> fp2Int(value, toType)
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.I16 -> {
                toType as SignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> sext(value, toType)
                    Type.I32 -> trunc(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8 ->  {
                        val zext = zext(value, Type.U16)
                        bitcast(zext, toType)
                    }
                    Type.U16 -> bitcast(value, toType)
                    Type.U32 -> {
                        val bitcast = bitcast(value, Type.I32)
                        trunc(bitcast, toType)
                    }
                    Type.U64 -> {
                        val bitcast = bitcast(value, Type.I64)
                        trunc(bitcast, toType)
                    }
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> fp2Int(value, toType)
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.I32 -> {
                toType as SignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> sext(value, toType)
                    Type.I16 -> sext(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8 -> {
                        val zext = zext(value, Type.U32)
                        bitcast(zext, Type.I32)
                    }

                    Type.U16 -> {
                        val zext = zext(value, Type.U32)
                        bitcast(zext, toType)
                    }

                    Type.U32 -> bitcast(value, toType)
                    Type.U64 -> {
                        val bitcast = bitcast(value, Type.I64)
                        trunc(bitcast, toType)
                    }
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> {
                        val tmp = fp2Int(value, Type.I64)
                        trunc(tmp, toType)
                    }
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value:${value.type()} to $toType")
                }
            }

            Type.I64 -> {
                toType as SignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> sext(value, toType)
                    Type.I16 -> sext(value, toType)
                    Type.I32 -> sext(value, toType)
                    Type.U8 -> {
                        val zext = zext(value, Type.U64)
                        bitcast(zext, toType)
                    }

                    Type.U16 -> {
                        val tmp = zext(value, Type.U64)
                        bitcast(tmp, toType)
                    }

                    Type.U32 -> {
                        val tmp = zext(value, Type.U64)
                        bitcast(tmp, toType)
                    }

                    Type.U64 -> bitcast(value, toType)
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> fp2Int(value, toType)
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.U8 -> {
                toType as UnsignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> bitcast(value, toType)
                    Type.I16 -> {
                        val bitcast = bitcast(value, Type.U16)
                        trunc(bitcast, toType)
                    }
                    Type.I32 -> {
                        val trunc = trunc(value, Type.I8)
                        bitcast(trunc, Type.U8)
                    }
                    Type.I64 -> {
                        val trunc = trunc(value, Type.I8)
                        bitcast(trunc, toType)
                    }
                    Type.U16 -> trunc(value, toType)
                    Type.U32 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> fp2Int(value, Type.U8)
                    Type.F64 -> fp2Int(value, Type.U8)
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.U16 -> {
                toType as UnsignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> {
                        val sext = sext(value, Type.I16)
                        bitcast(sext, toType)
                    }
                    Type.I16 -> bitcast(value, toType)
                    Type.I32 -> {
                        val bitcast = bitcast(value, Type.U32)
                        trunc(bitcast, toType)
                    }

                    Type.I64 -> {
                        val bitcast = bitcast(value, Type.U64)
                        trunc(bitcast, toType)
                    }
                    Type.U8 -> {
                        zext(value, toType)
                    }
                    Type.U32 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> {
                        val tmp = fp2Int(value, Type.I32)
                        trunc(tmp, toType)
                    }

                    Type.F64 -> fp2Int(value, Type.U16)
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.U32 -> {
                toType as UnsignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> {
                        val sext = sext(value, Type.I32)
                        bitcast(sext, toType)
                    }
                    Type.I16 -> {
                        val sext = sext(value, Type.I32)
                        bitcast(sext, toType)
                    }
                    Type.I32 -> bitcast(value, toType)
                    Type.I64 -> {
                        val bitcast = bitcast(value, Type.U64)
                        trunc(bitcast, Type.U32)
                    }
                    Type.U8  -> zext(value, toType)
                    Type.U16 -> zext(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> {
                        val tmp = fp2Int(value, Type.U64)
                        trunc(tmp, toType)
                    }
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.U64 -> {
                toType as UnsignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8 -> {
                        val sext = sext(value, Type.I64)
                        bitcast(sext, toType)
                    }
                    Type.I16 -> {
                        val sext = sext(value, Type.I64)
                        bitcast(sext, toType)
                    }
                    Type.I32 -> {
                        val tmp = sext(value, Type.I64)
                        bitcast(tmp, toType)
                    }
                    Type.I64 -> bitcast(value, toType)
                    Type.U8 -> zext(value, toType)
                    Type.U16 -> zext(value, toType)
                    Type.U32 -> zext(value, toType)
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> fp2Int(value, toType)
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.F32 -> {
                toType as FloatingPointType
                when (value.type()) {
                    Type.U1 -> int2fp(value, toType)
                    Type.I8 -> int2fp(value, toType)
                    Type.I16 -> int2fp(value, toType)
                    Type.I32 -> int2fp(value, toType)
                    Type.I64 -> int2fp(value, toType)
                    Type.U8 -> int2fp(value, toType)
                    Type.U16 -> uint2fp(value, toType)
                    Type.U32 -> uint2fp(value, toType)
                    Type.U64 -> int2fp(value, toType)
                    Type.F64 -> fptrunc(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.F64 -> {
                toType as FloatingPointType
                when (value.type()) {
                    Type.U1  -> int2fp(value, toType)
                    Type.I8  -> int2fp(value, toType)
                    Type.I16 -> int2fp(value, toType)
                    Type.I32 -> int2fp(value, toType)
                    Type.I64 -> int2fp(value, toType)
                    Type.U8  -> uint2fp(value, toType)
                    Type.U16 -> uint2fp(value, toType)
                    Type.U32 -> uint2fp(value, toType)
                    Type.U64 -> uint2fp(value, toType)
                    Type.F32 -> fpext(value, toType)
                    else -> throw RuntimeException("Cannot convert $value to $toType")
                }
            }

            Type.Ptr -> {
                toType as PointerType
                val valueType = value.type()
                if (valueType is IntegerType) {
                    return int2ptr(value)
                } else {
                    throw RuntimeException("Cannot convert $value:${valueType} to $toType")
                }
            }

            Type.U1 -> {
                when (val vType = value.type()) {
                    Type.I8  -> icmp(value, IntPredicate.Ne, I8Value(0))
                    Type.I16 -> icmp(value, IntPredicate.Ne, I16Value(0))
                    Type.I32 -> icmp(value, IntPredicate.Ne, I32Value(0))
                    Type.I64 -> icmp(value, IntPredicate.Ne, I64Value(0))
                    Type.U8  -> icmp(value, IntPredicate.Ne, U8Value(0))
                    Type.U16 -> icmp(value, IntPredicate.Ne, U16Value(0))
                    Type.U32 -> icmp(value, IntPredicate.Ne, U32Value(0))
                    Type.U64 -> icmp(value, IntPredicate.Ne, U64Value(0))
                    Type.Ptr -> icmp(value, IntPredicate.Ne, NullValue.NULLPTR)
                    else -> throw RuntimeException("Cannot convert $value:$vType to $toType")
                }
            }
            else -> throw RuntimeException("Cannot convert $value:${value.type()} to $toType")
        }
    }

    fun FunctionDataBuilder.storeCoerceArguments(structType: AnyCStructType, dst: Value, args: List<Value>) {
        when (val sizeOf = structType.size()) {
            BYTE_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val field = gep(dst, Type.I8, I64Value(0))
                store(field, args[0])
            }
            HWORD_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val field = gep(dst, Type.I16, I64Value(0))
                store(field, args[0])
            }
            WORD_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val loadedType = if (structType.hasFloatOnly(0, WORD_SIZE)) Type.F32 else Type.I32
                val field = gep(dst, loadedType, I64Value(0))
                store(field, args[0])
            }
            WORD_SIZE + BYTE_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val second = trunc(args[0], Type.I32)
                val shr    = shr(args[0], I64Value(WORD_SIZE * 8))
                val first  = trunc(shr, Type.I8)

                val field1 = gep(dst, Type.I32, I64Value(0))
                store(field1, second)

                val field2 = gep(dst, Type.I8, I64Value(WORD_SIZE))
                store(field2, first)
            }
            QWORD_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val loadedType = if (structType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
                val field = gep(dst, loadedType, I64Value( 0))
                store(field, args[0])
            }
            QWORD_SIZE + BYTE_SIZE -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

                val field = gep(dst, loadedType1, I64Value(0))
                store(field, args[0])

                val field1 = gep(dst, Type.I8, I64Value(QWORD_SIZE))
                store(field1, args[1])
            }
            QWORD_SIZE + HWORD_SIZE -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
                val fieldConverted = gep(dst, loadedType1, I64Value(0))
                store(fieldConverted, args[0])

                val fieldConverted1 = gep(dst, Type.I16, I64Value(QWORD_SIZE / Type.I16.sizeOf()))
                store(fieldConverted1, args[1])
            }
            QWORD_SIZE + WORD_SIZE -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
                val fieldConverted = gep(dst, loadedType1, I64Value(0))
                store(fieldConverted, args[0])

                val loadedType2 = if (structType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) Type.F32 else Type.I32
                val fieldConverted1 = gep(dst, loadedType2, I64Value(QWORD_SIZE / Type.I32.sizeOf()))
                store(fieldConverted1, args[1])
            }
            QWORD_SIZE * 2 -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
                val field = gep(dst, loadedType1, I64Value(0))
                store(field, args[0])

                val loadedType2 = if (structType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) Type.F64 else Type.I64
                val field1 = gep(dst, loadedType2, I64Value(1))
                store(field1, args[1])
            }
            else -> {
                assertion(!structType.isSmall()) {
                    "Cannot coerce arguments for size $sizeOf"
                }
                assertion(dst is Alloc) {
                    "Expected Alloc, but got $dst"
                }
            }
        }
    }

    fun FunctionDataBuilder.loadCoerceArguments(argCType: AnyCStructType, structPtr: Value): List<Value> = when (val sizeOf = argCType.size()) {
        BYTE_SIZE -> {
            val fieldConverted = gep(structPtr, Type.I8, I64Value(0))
            val load           = load(Type.I8, fieldConverted)
            arrayListOf(load)
        }
        HWORD_SIZE -> {
            val fieldConverted = gep(structPtr, Type.I16, I64Value(0))
            val load           = load(Type.I16, fieldConverted)
            arrayListOf(load)
        }
        HWORD_SIZE + BYTE_SIZE -> {
            val fieldConverted = gep(structPtr, Type.I8, I64Value(0))
            val load           = load(Type.I8, fieldConverted)

            val fieldConverted1 = gep(structPtr, Type.I16, I64Value(BYTE_SIZE))
            val load1           = load(Type.I16, fieldConverted1)


            val toInt1          = sext(load, Type.I32)
            val shr             = shl(toInt1, I32Value(HWORD_SIZE * 8))
            val toInt2          = sext(load1, Type.I32)
            val or              = or(toInt2, shr)
            arrayListOf(or)
        }
        WORD_SIZE -> {
            val loadedType = if (argCType.hasFloatOnly(0, WORD_SIZE)) Type.F32 else Type.I32
            val fieldConverted = gep(structPtr, loadedType, I64Value(0))
            val load           = load(loadedType, fieldConverted)
            arrayListOf(load)
        }
        WORD_SIZE + BYTE_SIZE -> {
            val field1 = gep(structPtr, Type.I32, I64Value(0))
            val load1  = load(Type.I32, field1)
            val sext1  = sext(load1, Type.I64)

            val field2 = gep(structPtr, Type.I8, I64Value(WORD_SIZE))
            val load2  = load(Type.I8, field2)
            val sext2  = sext(load2, Type.I64)

            val shl    = shl(sext2, I64Value(WORD_SIZE * 8))

            val and    = or(shl, sext1)
            arrayListOf(and)
        }
        QWORD_SIZE -> {
            val loadedType = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(structPtr, loadedType, I64Value( 0))
            val load           = load(loadedType, fieldConverted)
            arrayListOf(load)
        }
        QWORD_SIZE + BYTE_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(structPtr, loadedType1, I64Value(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val fieldConverted1 = gep(structPtr, Type.I8, I64Value(QWORD_SIZE))
            val load1           = load(Type.I8, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE + HWORD_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(structPtr, loadedType1, I64Value(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val fieldConverted1 = gep(structPtr, Type.I16, I64Value(QWORD_SIZE / Type.I16.sizeOf()))
            val load1           = load(Type.I16, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE + WORD_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(structPtr, loadedType1, I64Value(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val loadedType2 = if (argCType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) Type.F32 else Type.I32

            val fieldConverted1 = gep(structPtr, loadedType2, I64Value(QWORD_SIZE / Type.I32.sizeOf()))
            val load1           = load(loadedType2, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE * 2 -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            val fieldConverted = gep(structPtr, loadedType1, I64Value(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val loadedType2 = if (argCType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) Type.F64 else Type.I64
            val fieldConverted1 = gep(structPtr, loadedType2, I64Value(1))
            val load1           = load(loadedType2, fieldConverted1)
            values.add(load1)
            values
        }
        else -> {
            assertion(!argCType.isSmall()) {
                "Cannot coerce arguments for size $sizeOf"
            }
            assertion(structPtr is Alloc) {
                "Expected Alloc, but got $structPtr"
            }
            arrayListOf(structPtr)
        }
    }

    private fun convertConstant(value: PrimitiveConstant, type: Type): Value = when (type) {
        is PrimitiveType -> value.convertTo(type)
        is FlagType -> when (value) {
            is I8Value -> when (value.i8.toInt()) { //TODO toInt???
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is U8Value -> when (value.u8.toInt()) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is I16Value -> when (value.i16.toInt()) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is U16Value -> when (value.u16.toInt()) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is I32Value -> when (value.i32) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is U32Value -> when (value.u32) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is I64Value -> when (value.i64.toInt()) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            is U64Value -> when (value.u64.toInt()) {
                0 -> BoolValue.FALSE
                else -> BoolValue.TRUE
            }
            else -> throw RuntimeException("Cannot convert $value to $type")
        }
        else -> throw RuntimeException("Cannot convert $value to $type")
    }
}