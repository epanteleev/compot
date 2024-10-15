package gen

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
import ir.value.constant.Constant
import ir.value.constant.PrimitiveConstant
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
            throw IRCodeGenError("Cannot convert '$type' to ${T::class}")
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
        VOID   -> Type.Void // TODO handle case '(void) 0'
        is CStructType -> convertStructType(typeHolder, type)
        is CArrayType -> {
            val elementType = toIRType<NonTrivialType>(typeHolder, type.type.cType())
            ArrayType(elementType, type.dimension.toInt())
        }
        is CUnionType -> convertUnionType(typeHolder, type)
        is AnyCPointer -> Type.Ptr
        is CEnumType -> Type.I32
        is CFunctionType, is CUncompletedArrayType, is AbstractCFunction -> Type.Ptr
        else -> throw IRCodeGenError("Unknown type, type=$type")
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
        if (value.asType<NonTrivialType>().sizeOf() >= WORD_SIZE) {
            return value
        }
        return convertToType(value, Type.I64)
    }

    fun FunctionDataBuilder.convertToType(value: Value, toType: Type): Value {
        if (value.type() == toType) {
            return value
        }
        if (value is PrimitiveConstant && toType !is PointerType) {
            // Opt IR does not support pointer constants.
            return convertConstant(value, toType as NonTrivialType)
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value:${value.type()} to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
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
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
                }
            }

            Type.Ptr -> {
                toType as PointerType
                val valueType = value.type()
                if (valueType is IntegerType) {
                    return int2ptr(value)
                } else {
                    throw IRCodeGenError("Cannot convert $value:${valueType} to $toType")
                }
            }

            Type.U1 -> {
                when (value.type()) {
                    Type.I8  -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.I8, 0))
                    Type.I16 -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.I16, 0))
                    Type.I32 -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.I32, 0))
                    Type.I64 -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.I64, 0))
                    Type.U8  -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.U8, 0))
                    Type.U16 -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.U16, 0))
                    Type.U32 -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.U32, 0))
                    Type.U64 -> icmp(value, IntPredicate.Ne, Constant.valueOf(Type.U64, 0))
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
                }
            }
            else -> throw IRCodeGenError("Cannot convert $value:${value.type()} to $toType")
        }
    }

    fun FunctionDataBuilder.coerceArguments(argCType: AnyCStructType, expr: Value): List<Value> = when (argCType.size()) {
        BYTE_SIZE -> {
            val fieldConverted = gep(expr, Type.I8, Constant.valueOf(Type.I64, 0))
            val load           = load(Type.I8, fieldConverted)
            arrayListOf(load)
        }
        HWORD_SIZE -> {
            val fieldConverted = gep(expr, Type.I16, Constant.valueOf(Type.I64, 0))
            val load           = load(Type.I16, fieldConverted)
            arrayListOf(load)
        }
        WORD_SIZE -> {
            val loadedType = if (argCType.hasFloatOnly(0, WORD_SIZE)) Type.F32 else Type.I32
            val fieldConverted = gep(expr, loadedType, Constant.valueOf(Type.I64, 0))
            val load           = load(loadedType, fieldConverted)
            arrayListOf(load)
        }
        QWORD_SIZE -> {
            val loadedType = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(expr, loadedType, Constant.valueOf(Type.I64, 0))
            val load           = load(loadedType, fieldConverted)
            arrayListOf(load)
        }
        QWORD_SIZE + BYTE_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(expr, loadedType1, Constant.valueOf(Type.I64, 0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val fieldConverted1 = gep(expr, Type.I8, Constant.valueOf(Type.I64, QWORD_SIZE))
            val load1           = load(Type.I8, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE + HWORD_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(expr, loadedType1, Constant.valueOf(Type.I64, 0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val fieldConverted1 = gep(expr, Type.I16, Constant.valueOf(Type.I64, QWORD_SIZE / Type.I16.sizeOf()))
            val load1           = load(Type.I16, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE + WORD_SIZE -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64

            val fieldConverted = gep(expr, loadedType1, Constant.valueOf(Type.I64, 0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val loadedType2 = if (argCType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) Type.F32 else Type.I32

            val fieldConverted1 = gep(expr, loadedType2, Constant.valueOf(Type.I64, QWORD_SIZE / Type.I32.sizeOf()))
            val load1           = load(loadedType2, fieldConverted1)
            values.add(load1)
            values
        }
        QWORD_SIZE * 2 -> {
            val loadedType1 = if (argCType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            val fieldConverted = gep(expr, loadedType1, Constant.valueOf(Type.I64, 0))
            val load           = load(loadedType1, fieldConverted)
            val values = arrayListOf(load)

            val loadedType2 = if (argCType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) Type.F64 else Type.I64
            val fieldConverted1 = gep(expr, loadedType2, Constant.valueOf(Type.I64, 1))
            val load1           = load(loadedType2, fieldConverted1)
            values.add(load1)
            values
        }
        else -> {
            assertion(expr is Alloc) {
                "Expected Alloc, but got $expr"
            }
            arrayListOf(expr)
        }
    }

    private fun convertConstant(value: PrimitiveConstant, type: NonTrivialType): Value {
        return PrimitiveConstant.from(type, value)
    }
}