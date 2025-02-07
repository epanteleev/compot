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


internal object TypeConverter {
    inline fun <reified T : NonTrivialType> ModuleBuilder.toIRLVType(typeHolder: TypeHolder, type: CType): T {
        val converted = toIRType<Type>(typeHolder, type)
        return if (converted == FlagType) {
            I8Type as T
        } else {
            converted as T
        }
    }

    inline fun <reified T : Type> ModuleBuilder.toIRType(typeHolder: TypeHolder, type: CType): T {
        val converted = toIRTypeUnchecked(typeHolder, type)
        if (type is AnyCStructType) { //TODO debug only check. Remove it in the future
            converted as StructType
            if (type.size() != converted.sizeOf()) {
                throw  IllegalStateException("Size mismatch, expected ${type.size()}, but got ${converted.sizeOf()}: ctype=$type, irType=$converted")
            }
        }

        if (converted !is T) {
            throw RuntimeException("Cannot convert '$type' to ${T::class}")
        }

        return converted
    }

    fun ModuleBuilder.toIRTypeUnchecked(typeHolder: TypeHolder, type: CType): Type = when (type) {
        BOOL   -> FlagType
        CHAR   -> I8Type
        UCHAR  -> U8Type
        SHORT  -> I16Type
        USHORT -> U16Type
        INT    -> I32Type
        UINT   -> U32Type
        LONG   -> I64Type
        ULONG  -> U64Type
        FLOAT  -> F32Type
        DOUBLE -> F64Type
        VOID   -> VoidType
        is CStructType -> convertStructType(typeHolder, type)
        is CArrayType -> {
            val elementType = toIRLVType<NonTrivialType>(typeHolder, type.type.cType())
            ArrayType(elementType, type.dimension.toInt())
        }
        is CStringLiteral -> {
            val elementType = toIRLVType<NonTrivialType>(typeHolder, type.type.cType())
            ArrayType(elementType, type.dimension.toInt())
        }
        is CUnionType -> convertUnionType(typeHolder, type)
        is CPointer   -> PtrType
        is CEnumType  -> I32Type
        is CFunctionType, is AbstractCFunction -> PtrType
        else -> throw RuntimeException("Unknown type, type=$type, class=${type::class}")
    }

    private fun ModuleBuilder.convertStructType(typeHolder: TypeHolder, type: AnyCStructType): Type {
        val structType = findStructTypeOrNull(type.name)
        if (structType != null) {
            return structType
        }

        val fields = arrayListOf<NonTrivialType>()
        for (member in type.members()) {
            val cType = member.cType()
            if (cType is CUncompletedArrayType) { //TODO check that this is last member
                continue
            }

            val irType = toIRLVType<NonTrivialType>(typeHolder, member.cType())
            fields.add(irType)
        }

        return structType(type.name, fields)
    }

    private fun ModuleBuilder.convertUnionType(typeHolder: TypeHolder, type: CUnionType): Type {
        val structType = findStructTypeOrNull(type.name)
        if (structType != null) {
            return structType
        }

        val field = type.members().maxByOrNull { it.cType().asType<CompletedType>().size() }.let {
            if (it == null) {
                null
            } else {
                toIRLVType<NonTrivialType>(typeHolder, it.cType())
            }
        }
        return if (field == null) {
            structType(type.name, listOf(), type.alignmentOf())
        } else {
            structType(type.name, listOf(field), type.alignmentOf())
        }
    }

    fun FunctionDataBuilder.convertRVToType(rvalue: Value, toType: Type): Value {
        val rightCvt = convertLVToType(rvalue, toType)
        return when (toType) {
            FlagType -> flag2int(rightCvt, I8Type)
            else     -> rightCvt
        }
    }

    fun FunctionDataBuilder.convertLVToType(lvalue: Value, toType: Type): Value {
        if (lvalue.type() == toType) {
            return lvalue
        }
        if (lvalue is PrimitiveConstant && toType !is PtrType) {
            // Opt IR does not support pointer constants.
            return convertConstant(lvalue, toType)
        }

        return when (toType) {
            I8Type -> {
                toType as SignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I16Type -> trunc(lvalue, toType)
                    I32Type -> trunc(lvalue, toType)
                    I64Type -> trunc(lvalue, toType)
                    U8Type  -> bitcast(lvalue, toType)
                    U16Type -> trunc(lvalue, toType)
                    U32Type -> {
                        val trunc = trunc(lvalue, U8Type)
                        bitcast(trunc, toType)
                    }
                    U64Type -> {
                        val bitcast = bitcast(lvalue, I64Type)
                        trunc(bitcast, toType)
                    }
                    F32Type -> fp2Int(lvalue, toType)
                    F64Type -> fp2Int(lvalue, toType)
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            I16Type -> {
                toType as SignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type  -> sext(lvalue, toType)
                    I32Type -> trunc(lvalue, toType)
                    I64Type -> trunc(lvalue, toType)
                    U8Type  -> {
                        val zext = zext(lvalue, U16Type)
                        bitcast(zext, toType)
                    }
                    U16Type -> bitcast(lvalue, toType)
                    U32Type -> {
                        val bitcast = bitcast(lvalue, I32Type)
                        trunc(bitcast, toType)
                    }
                    U64Type -> {
                        val bitcast = bitcast(lvalue, I64Type)
                        trunc(bitcast, toType)
                    }
                    F32Type -> fp2Int(lvalue, toType)
                    F64Type -> fp2Int(lvalue, toType)
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            I32Type -> {
                toType as SignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type -> sext(lvalue, toType)
                    I16Type -> sext(lvalue, toType)
                    I64Type -> trunc(lvalue, toType)
                    U8Type -> {
                        val zext = zext(lvalue, U32Type)
                        bitcast(zext, I32Type)
                    }

                    U16Type  -> {
                        val zext = zext(lvalue, U32Type)
                        bitcast(zext, toType)
                    }

                    U32Type -> bitcast(lvalue, toType)
                    U64Type -> {
                        val bitcast = bitcast(lvalue, I64Type)
                        trunc(bitcast, toType)
                    }
                    F32Type -> fp2Int(lvalue, toType)
                    F64Type -> {
                        val tmp = fp2Int(lvalue, I64Type)
                        trunc(tmp, toType)
                    }
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue:${lvalue.type()} to $toType")
                }
            }

            I64Type -> {
                toType as SignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type -> sext(lvalue, toType)
                    I16Type -> sext(lvalue, toType)
                    I32Type -> sext(lvalue, toType)
                    U8Type -> {
                        val zext = zext(lvalue, U64Type)
                        bitcast(zext, toType)
                    }

                    U16Type -> {
                        val tmp = zext(lvalue, U64Type)
                        bitcast(tmp, toType)
                    }

                    U32Type -> {
                        val tmp = zext(lvalue, U64Type)
                        bitcast(tmp, toType)
                    }

                    U64Type -> bitcast(lvalue, toType)
                    F32Type -> fp2Int(lvalue, toType)
                    F64Type -> fp2Int(lvalue, toType)
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            U8Type -> {
                toType as UnsignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type -> bitcast(lvalue, toType)
                    I16Type -> {
                        val bitcast = bitcast(lvalue, U16Type)
                        trunc(bitcast, toType)
                    }
                    I32Type -> {
                        val trunc = trunc(lvalue, I8Type)
                        bitcast(trunc, U8Type)
                    }
                    I64Type -> {
                        val trunc = trunc(lvalue, I8Type)
                        bitcast(trunc, toType)
                    }
                    U16Type -> trunc(lvalue, toType)
                    U32Type -> trunc(lvalue, toType)
                    U64Type -> trunc(lvalue, toType)
                    F32Type -> fp2Int(lvalue, U8Type)
                    F64Type -> fp2Int(lvalue, U8Type)
                    PtrType  -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            U16Type -> {
                toType as UnsignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type -> {
                        val sext = sext(lvalue, I16Type)
                        bitcast(sext, toType)
                    }
                    I16Type -> bitcast(lvalue, toType)
                    I32Type -> {
                        val bitcast = bitcast(lvalue, U32Type)
                        trunc(bitcast, toType)
                    }

                    I64Type -> {
                        val bitcast = bitcast(lvalue, U64Type)
                        trunc(bitcast, toType)
                    }
                    U8Type -> {
                        zext(lvalue, toType)
                    }
                    U32Type -> trunc(lvalue, toType)
                    U64Type -> trunc(lvalue, toType)
                    F32Type -> {
                        val tmp = fp2Int(lvalue, I32Type)
                        trunc(tmp, toType)
                    }

                    F64Type -> fp2Int(lvalue, U16Type)
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            U32Type -> {
                toType as UnsignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type -> {
                        val sext = sext(lvalue, I32Type)
                        bitcast(sext, toType)
                    }
                    I16Type -> {
                        val sext = sext(lvalue, I32Type)
                        bitcast(sext, toType)
                    }
                    I32Type -> bitcast(lvalue, toType)
                    I64Type -> {
                        val bitcast = bitcast(lvalue, U64Type)
                        trunc(bitcast, U32Type)
                    }
                    U8Type  -> zext(lvalue, toType)
                    U16Type -> zext(lvalue, toType)
                    U64Type -> trunc(lvalue, toType)
                    F32Type -> fp2Int(lvalue, toType)
                    F64Type -> {
                        val tmp = fp2Int(lvalue, U64Type)
                        trunc(tmp, toType)
                    }
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            U64Type -> {
                toType as UnsignedIntType
                when (lvalue.type()) {
                    FlagType -> flag2int(lvalue, toType)
                    I8Type -> {
                        val sext = sext(lvalue, I64Type)
                        bitcast(sext, toType)
                    }
                    I16Type -> {
                        val sext = sext(lvalue, I64Type)
                        bitcast(sext, toType)
                    }
                    I32Type -> {
                        val tmp = sext(lvalue, I64Type)
                        bitcast(tmp, toType)
                    }
                    I64Type -> bitcast(lvalue, toType)
                    U8Type  -> zext(lvalue, toType)
                    U16Type -> zext(lvalue, toType)
                    U32Type -> zext(lvalue, toType)
                    F32Type -> fp2Int(lvalue, toType)
                    F64Type -> fp2Int(lvalue, toType)
                    PtrType -> ptr2int(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            F32Type -> {
                toType as FloatingPointType
                when (lvalue.type()) {
                    FlagType -> int2fp(lvalue, toType)
                    I8Type  -> int2fp(lvalue, toType)
                    I16Type -> int2fp(lvalue, toType)
                    I32Type -> int2fp(lvalue, toType)
                    I64Type -> int2fp(lvalue, toType)
                    U8Type  -> int2fp(lvalue, toType)
                    U16Type -> uint2fp(lvalue, toType)
                    U32Type -> uint2fp(lvalue, toType)
                    U64Type -> int2fp(lvalue, toType)
                    F64Type -> fptrunc(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            F64Type -> {
                toType as FloatingPointType
                when (lvalue.type()) {
                    FlagType -> int2fp(lvalue, toType)
                    I8Type  -> int2fp(lvalue, toType)
                    I16Type -> int2fp(lvalue, toType)
                    I32Type -> int2fp(lvalue, toType)
                    I64Type -> int2fp(lvalue, toType)
                    U8Type  -> uint2fp(lvalue, toType)
                    U16Type -> uint2fp(lvalue, toType)
                    U32Type -> uint2fp(lvalue, toType)
                    U64Type -> uint2fp(lvalue, toType)
                    F32Type -> fpext(lvalue, toType)
                    else -> throw RuntimeException("Cannot convert $lvalue to $toType")
                }
            }

            PtrType -> {
                toType as PtrType
                val valueType = lvalue.type()
                if (valueType is IntegerType) {
                    return int2ptr(lvalue)
                } else {
                    throw RuntimeException("Cannot convert $lvalue:${valueType} to $toType")
                }
            }

            FlagType -> when (val vType = lvalue.type()) {
                I8Type  -> icmp(lvalue, IntPredicate.Ne, I8Value.of(0))
                I16Type -> icmp(lvalue, IntPredicate.Ne, I16Value.of(0))
                I32Type -> icmp(lvalue, IntPredicate.Ne, I32Value.of(0))
                I64Type -> icmp(lvalue, IntPredicate.Ne, I64Value.of(0))
                U8Type  -> icmp(lvalue, IntPredicate.Ne, U8Value.of(0))
                U16Type -> icmp(lvalue, IntPredicate.Ne, U16Value.of(0))
                U32Type -> icmp(lvalue, IntPredicate.Ne, U32Value.of(0))
                U64Type -> icmp(lvalue, IntPredicate.Ne, U64Value.of(0))
                PtrType -> icmp(lvalue, IntPredicate.Ne, NullValue)
                else -> throw RuntimeException("Cannot convert $lvalue:$vType to $toType")
            }
            else -> throw RuntimeException("Cannot convert $lvalue:${lvalue.type()} to $toType")
        }
    }

    fun FunctionDataBuilder.storeCoerceArguments(structType: AnyCStructType, dst: Value, args: List<Value>) {
        when (val sizeOf = structType.size()) {
            BYTE_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val field = gep(dst, I8Type, I64Value.of(0))
                store(field, args[0])
            }
            HWORD_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val field = gep(dst, I16Type, I64Value.of(0))
                store(field, args[0])
            }
            HWORD_SIZE + BYTE_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                assertion(args[0].type() == I32Type) { "invariant: args=$args" }

                val second = trunc(args[0], I16Type)
                val field1 = gep(dst, I16Type, I64Value.of(0))
                store(field1, second)

                val shr    = shr(args[0], I32Value.of(HWORD_SIZE * 8))
                val first  = trunc(shr, I8Type)
                val field2 = gep(dst, I8Type, I64Value.of(HWORD_SIZE))
                store(field2, first)
            }
            WORD_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val loadedType = if (structType.hasFloatOnly(0, WORD_SIZE)) F32Type else I32Type
                val field = gep(dst, loadedType, I64Value.of(0))
                store(field, args[0])
            }
            WORD_SIZE + BYTE_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                assertion(args[0].type() == I64Type) { "invariant: args=$args" }

                val second = trunc(args[0], I32Type)
                val field1 = gep(dst, I32Type, I64Value.of(0))
                store(field1, second)

                val shr    = shr(args[0], I64Value.of(WORD_SIZE * 8))
                val first  = trunc(shr, I8Type)
                val field2 = gep(dst, I8Type, I64Value.of(WORD_SIZE))
                store(field2, first)
            }
            QWORD_SIZE -> {
                assertion(args.size == 1) { "invariant: args=$args" }
                val loadedType = if (structType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
                val field = gep(dst, loadedType, I64Value.of( 0))
                store(field, args[0])
            }
            QWORD_SIZE + BYTE_SIZE -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type

                val field = gep(dst, loadedType1, I64Value.of(0))
                store(field, args[0])

                val field1 = gep(dst, I8Type, I64Value.of(QWORD_SIZE))
                store(field1, args[1])
            }
            QWORD_SIZE + HWORD_SIZE -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
                val fieldConverted = gep(dst, loadedType1, I64Value.of(0))
                store(fieldConverted, args[0])

                val fieldConverted1 = gep(dst, I16Type, I64Value.of(QWORD_SIZE / I16Type.sizeOf()))
                store(fieldConverted1, args[1])
            }
            QWORD_SIZE + WORD_SIZE -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
                val fieldConverted = gep(dst, loadedType1, I64Value.of(0))
                store(fieldConverted, args[0])

                val loadedType2 = if (structType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) F32Type else I32Type
                val fieldConverted1 = gep(dst, loadedType2, I64Value.of(QWORD_SIZE / I32Type.sizeOf()))
                store(fieldConverted1, args[1])
            }
            QWORD_SIZE * 2 -> {
                assertion(args.size == 2) { "invariant: args=$args" }
                val loadedType1 = if (structType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
                val field = gep(dst, loadedType1, I64Value.of(0))
                store(field, args[0])

                val loadedType2 = if (structType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) F64Type else I64Type
                val field1 = gep(dst, loadedType2, I64Value.of(1))
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
            val fieldConverted = gep(structPtr, I8Type, I64Value.of(0))
            val load           = load(I8Type, fieldConverted)
            arrayListOf(load)
        }
        HWORD_SIZE -> {
            val fieldConverted = gep(structPtr, I16Type, I64Value.of(0))
            val load           = load(I16Type, fieldConverted)
            arrayListOf(load)
        }
        HWORD_SIZE + BYTE_SIZE -> {
            val field  = gep(structPtr, I16Type, I64Value.of(0))
            val load   = load(I16Type, field)
            val toInt1 = sext(load, I32Type)

            val field1 = gep(structPtr, I8Type, I64Value.of(HWORD_SIZE))
            val load1  = load(I8Type, field1)
            val toInt2 = sext(load1, I32Type)

            val shr = shl(toInt2, I32Value.of(HWORD_SIZE * 8))
            val or  = or(shr, toInt1)
            arrayListOf(or)
        }
        WORD_SIZE -> {
            val loadedType = if (argCType.hasFloatOnly(0, WORD_SIZE)) F32Type else I32Type
            val fieldConverted = gep(structPtr, loadedType, I64Value.of(0))
            val load           = load(loadedType, fieldConverted)
            arrayListOf(load)
        }
        WORD_SIZE + BYTE_SIZE -> {
            val field1 = gep(structPtr, I32Type, I64Value.of(0))
            val load1  = load(I32Type, field1)
            val sext1  = sext(load1, I64Type)

            val field2 = gep(structPtr, I8Type, I64Value.of(WORD_SIZE))
            val load2  = load(I8Type, field2)
            val sext2  = sext(load2, I64Type)

            val shl    = shl(sext2, I64Value.of(WORD_SIZE * 8))
            val and    = or(shl, sext1)
            arrayListOf(and)
        }
        QWORD_SIZE -> {
            val loadedType = if (argCType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type

            val fieldConverted = gep(structPtr, loadedType, I64Value.of( 0))
            val load           = load(loadedType, fieldConverted)
            arrayListOf(load)
        }
        QWORD_SIZE + BYTE_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type

            val fieldConverted = gep(structPtr, loadedType1, I64Value.of(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val fieldConverted1 = gep(structPtr, I8Type, I64Value.of(QWORD_SIZE))
            val load1           = load(I8Type, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE + HWORD_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type

            val fieldConverted = gep(structPtr, loadedType1, I64Value.of(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val fieldConverted1 = gep(structPtr, I16Type, I64Value.of(QWORD_SIZE / I16Type.sizeOf()))
            val load1           = load(I16Type, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE + WORD_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type

            val fieldConverted = gep(structPtr, loadedType1, I64Value.of(0))
            val load           = load(loadedType1, fieldConverted)
            val values         = arrayListOf(load)

            val loadedType2 = if (argCType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) F32Type else I32Type

            val fieldConverted1 = gep(structPtr, loadedType2, I64Value.of(QWORD_SIZE / I32Type.sizeOf()))
            val load1           = load(loadedType2, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE * 2 -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            val fieldConverted = gep(structPtr, loadedType1, I64Value.of(0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val loadedType2 = if (argCType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) F64Type else I64Type
            val fieldConverted1 = gep(structPtr, loadedType2, I64Value.of(1))
            val load1           = load(loadedType2, fieldConverted1)
            values.add(load1)
            values
        }
        else -> {
            assertion(!argCType.isSmall()) {
                "Cannot coerce arguments for size $sizeOf"
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