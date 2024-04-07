package gen

import ir.Constant
import ir.Value
import ir.module.builder.impl.FunctionDataBuilder
import ir.types.SignedIntType
import ir.types.Type
import ir.types.UnsignedIntType

object TypeConverter {

    fun interfereTypes(a: Value, b: Value): Type {
        val aType = a.type()
        val bType = b.type()
        if (aType == bType) {
            return aType
        }

        return when (aType) {
            Type.I8 -> {
                when (bType) {
                    Type.I16 -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.I8
                    Type.U16 -> Type.I16
                    Type.U32 -> Type.I32
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.I16 -> {
                when (bType) {
                    Type.I8  -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.I16
                    Type.U16 -> Type.I16
                    Type.U32 -> Type.I32
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.I32 -> {
                when (bType) {
                    Type.I8  -> Type.I32
                    Type.I16 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.I32
                    Type.U16 -> Type.I32
                    Type.U32 -> Type.I32
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.I64 -> {
                when (bType) {
                    Type.I8  -> Type.I64
                    Type.I16 -> Type.I64
                    Type.I32 -> Type.I64
                    Type.U8  -> Type.I64
                    Type.U16 -> Type.I64
                    Type.U32 -> Type.I64
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U8 -> {
                when (bType) {
                    Type.I8  -> Type.I8
                    Type.I16 -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U16 -> Type.U16
                    Type.U32 -> Type.U32
                    Type.U64 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U16 -> {
                when (bType) {
                    Type.I8  -> Type.I16
                    Type.I16 -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.U16
                    Type.U32 -> Type.U32
                    Type.U64 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U32 -> {
                when (bType) {
                    Type.I8  -> Type.I32
                    Type.I16 -> Type.I32
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.U32
                    Type.U16 -> Type.U32
                    Type.U64 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U64 -> {
                when (bType) {
                    Type.I8  -> Type.I64
                    Type.I16 -> Type.I64
                    Type.I32 -> Type.I64
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.U64
                    Type.U16 -> Type.U64
                    Type.U32 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.F32 -> {
                when (bType) {
                    Type.I8  -> Type.F32
                    Type.I16 -> Type.F32
                    Type.I32 -> Type.F32
                    Type.I64 -> Type.F32
                    Type.U8  -> Type.F32
                    Type.U16 -> Type.F32
                    Type.U32 -> Type.F32
                    Type.U64 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.F64 -> {
                when (bType) {
                    Type.I8  -> Type.F64
                    Type.I16 -> Type.F64
                    Type.I32 -> Type.F64
                    Type.I64 -> Type.F64
                    Type.U8  -> Type.F64
                    Type.U16 -> Type.F64
                    Type.U32 -> Type.F64
                    Type.U64 -> Type.F64
                    Type.F32 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
        }
    }

   fun FunctionDataBuilder.convertToType(value: Value, type: Type): Value {
        if (value.type() == type) {
            return value
        }
        if (value is Constant) {
            return convertConstant(value, type)
        }

        return when (type) {
            Type.I8 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I16 -> trunc(value, type)
                    Type.I32 -> trunc(value, type)
                    Type.I64 -> trunc(value, type)
                    Type.U8  -> trunc(value, type)
                    Type.U16 -> trunc(value, type)
                    Type.U32 -> trunc(value, type)
                    Type.U64 -> trunc(value, type)
                    Type.F32 -> fptosi(value, type)
                    Type.F64 -> fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.I16 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I8  -> sext(value, type)
                    Type.I32 -> trunc(value, type)
                    Type.I64 -> trunc(value, type)
                    Type.U8  -> sext(value, type)
                    Type.U16 -> bitcast(value, type)
                    Type.U32 -> trunc(value, type)
                    Type.U64 -> trunc(value, type)
                    Type.F32 -> fptosi(value, type)
                    Type.F64 -> fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.I32 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I8 -> sext(value, type)
                    Type.I16 -> sext(value, type)
                    Type.I64 -> trunc(value, type)
                    Type.U8  -> {
                        val tmp = sext(value, Type.I32)
                        trunc(tmp, type)
                    }
                    Type.U16 -> {
                        val tmp = zext(value, Type.U32)
                        trunc(tmp, type)
                    }
                    Type.U32 -> bitcast(value, type)
                    Type.U64 -> trunc(value, type)
                    Type.F32 -> fptosi(value, type)
                    Type.F64 -> fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.I64 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I8 -> sext(value, type)
                    Type.I16 -> sext(value, type)
                    Type.I32 -> sext(value, type)
                    Type.U8  -> {
                        val tmp = sext(value, Type.I64)
                        trunc(tmp, type)
                    }
                    Type.U16 -> {
                        val tmp = zext(value, Type.U64)
                        trunc(tmp, type)
                    }
                    Type.U32 -> {
                        val tmp = zext(value, Type.U64)
                        trunc(tmp, type)
                    }
                    Type.U64 -> bitcast(value, type)
                    Type.F32 -> fptosi(value, type)
                    Type.F64 -> fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.U8 -> {
                type as UnsignedIntType
                when (value.type()) {
                    Type.I8  -> bitcast(value, type)
                    Type.I16 -> trunc(value, type)
                    Type.I32 -> trunc(value, type)
                    Type.I64 -> trunc(value, type)
                    Type.U16 -> trunc(value, type)
                    Type.U32 -> trunc(value, type)
                    Type.U64 -> trunc(value, type)
                    Type.F32 -> {
                        val tmp = fptosi(value, Type.I32)
                        trunc(tmp, type)
                    }
                    Type.F64 -> {
                        val tmp = fptosi(value, Type.I64)
                        trunc(tmp, type)
                    }
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.U16 -> {
                type as UnsignedIntType
                when (value.type()) {
                    Type.I8  -> trunc(value, type)
                    Type.I16 -> bitcast(value, type)
                    Type.I32 -> trunc(value, type)
                    Type.I64 -> trunc(value, type)
                    Type.U8  -> trunc(value, type)
                    Type.U32 -> trunc(value, type)
                    Type.U64 -> trunc(value, type)
                    Type.F32 -> {
                        val tmp = fptosi(value, Type.I32)
                        trunc(tmp, type)
                    }
                    Type.F64 -> {
                        val tmp = fptosi(value, Type.I64)
                        trunc(tmp, type)
                    }
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.U32 -> {
                type as UnsignedIntType
                when (value.type()) {
                    Type.I8  -> trunc(value, type)
                    Type.I16 -> trunc(value, type)
                    Type.I32 -> bitcast(value, type)
                    Type.I64 -> trunc(value, type)
                    Type.U8  -> trunc(value, type)
                    Type.U16 -> trunc(value, type)
                    Type.U64 -> trunc(value, type)
                    Type.F32 -> {
                        val tmp = fptosi(value, Type.I32)
                        trunc(tmp, type)
                    }
                    Type.F64 -> {
                        val tmp = fptosi(value, Type.I64)
                        trunc(tmp, type)
                    }
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }
            else -> throw IRCodeGenError("Cannot convert $value to $type")
        }
    }

    private fun convertConstant(value: Constant, type: Type): Value {
        return Constant.from(type, value)
    }
}