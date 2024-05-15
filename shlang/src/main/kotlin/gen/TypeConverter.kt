package gen

import ir.*
import types.*
import ir.types.*
import ir.module.builder.impl.FunctionDataBuilder


object TypeConverter {

    inline fun<reified T: Type> toIRType(type: CType): T {
        return toIRTypeUnchecked(type) as T
    }

    fun toIRTypeUnchecked(type: CType): Type {
        for (p in type.qualifiers()) {
            if (p is PointerQualifier) {
                return Type.Ptr
            }
        }
        if (type is CPointerType) {
            return Type.Ptr
        }

        if (type is CompoundType) {
            val baseType = type.baseType()
            if (baseType is CArrayType) {
                return ArrayType(toIRType<NonTrivialType>(baseType.type), baseType.dimension)
            }
        }

        val ret = when (type.baseType()) {
            CPrimitive.CHAR -> Type.I8
            CPrimitive.SHORT -> Type.I16
            CPrimitive.INT -> Type.I32
            CPrimitive.LONG -> Type.I64
            CPrimitive.FLOAT -> Type.F32
            CPrimitive.DOUBLE -> Type.F64
            CPrimitive.VOID -> Type.Void
            else -> throw IRCodeGenError("Unknown type, type=$type")
        }
        return ret
    }

   fun FunctionDataBuilder.convertToType(value: Value, toType: Type): Value {
        if (value.type() == toType) {
            return value
        }
        if (value is Constant) {
            return convertConstant(value, toType)
        }

        return when (toType) {
            Type.I8 -> {
                toType as SignedIntType
                when (value.type()) {
                    Type.U1  -> flag2int(value, toType)
                    Type.I16 -> trunc(value, toType)
                    Type.I32 -> trunc(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8  -> trunc(value, toType)
                    Type.U16 -> trunc(value, toType)
                    Type.U32 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
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
                    Type.I8  -> sext(value, toType)
                    Type.I32 -> trunc(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8  -> sext(value, toType)
                    Type.U16 -> bitcast(value, toType)
                    Type.U32 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
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
                    Type.U8  -> {
                        val bitcast = bitcast(value, Type.I8)
                        sext(bitcast, Type.I32)
                    }
                    Type.U16 -> {
                        val tmp = zext(value, Type.U32)
                        trunc(tmp, toType)
                    }
                    Type.U32 -> bitcast(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> fp2Int(value, toType)
                    Type.F64 -> fp2Int(value, toType)
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
                    Type.U8  -> {
                        val tmp = sext(value, Type.I64)
                        trunc(tmp, toType)
                    }
                    Type.U16 -> {
                        val tmp = zext(value, Type.U64)
                        trunc(tmp, toType)
                    }
                    Type.U32 -> {
                        val tmp = zext(value, Type.U64)
                        trunc(tmp, toType)
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
                    Type.I8  -> bitcast(value, toType)
                    Type.I16 -> trunc(value, toType)
                    Type.I32 -> trunc(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U16 -> trunc(value, toType)
                    Type.U32 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> {
                        val tmp = fp2Int(value, Type.I32)
                        trunc(tmp, toType)
                    }
                    Type.F64 -> {
                        val tmp = fp2Int(value, Type.I64)
                        trunc(tmp, toType)
                    }
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
                }
            }

            Type.U16 -> {
                toType as UnsignedIntType
                when (value.type()) {
                    Type.U1 -> flag2int(value, toType)
                    Type.I8  -> trunc(value, toType)
                    Type.I16 -> bitcast(value, toType)
                    Type.I32 -> trunc(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8  -> trunc(value, toType)
                    Type.U32 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> {
                        val tmp = fp2Int(value, Type.I32)
                        trunc(tmp, toType)
                    }
                    Type.F64 -> {
                        val tmp = fp2Int(value, Type.I64)
                        trunc(tmp, toType)
                    }
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
                }
            }

            Type.U32 -> {
                toType as UnsignedIntType
                when (value.type()) {
                    Type.U1  -> flag2int(value, toType)
                    Type.I8  -> trunc(value, toType)
                    Type.I16 -> trunc(value, toType)
                    Type.I32 -> bitcast(value, toType)
                    Type.I64 -> trunc(value, toType)
                    Type.U8  -> trunc(value, toType)
                    Type.U16 -> trunc(value, toType)
                    Type.U64 -> trunc(value, toType)
                    Type.F32 -> {
                        val tmp = fp2Int(value, Type.I32)
                        trunc(tmp, toType)
                    }
                    Type.F64 -> {
                        val tmp = fp2Int(value, Type.I64)
                        trunc(tmp, toType)
                    }
                    Type.Ptr -> ptr2int(value, toType)
                    else -> throw IRCodeGenError("Cannot convert $value to $toType")
                }
            }
            Type.Ptr -> {
                toType as PointerType
                val valueType = value.type()
                if (valueType is IntegerType) {
                    return int2ptr(value)
                } else {
                    throw IRCodeGenError("Cannot convert $value to $toType")
                }
            }
            else -> throw IRCodeGenError("Cannot convert $value:${value.type()} to $toType")
        }
    }

    private fun convertConstant(value: Constant, type: Type): Value {
        return Constant.from(type, value)
    }
}