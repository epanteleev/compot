package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.ArithmeticType
import ir.types.PrimitiveType
import ir.types.Type

enum class CastType {
    ZeroExtend {
        override fun toString(): String {
            return "zext"
        }
    },
    SignExtend {
        override fun toString(): String {
            return "sext"
        }
    },
    Truncate {
        override fun toString(): String {
            return "trunc"
        }
    },
    Bitcast {
        override fun toString(): String {
            return "bitcast"
        }
    };
}

class Cast private constructor(name: String, ty: Type, val castType: CastType, value: Value):
    ValueInstruction(name, ty, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $castType ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun copy(newUsages: List<Value>): Cast {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, type(), castType, newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, ty: PrimitiveType, castType: CastType, value: Value): Cast {
            val valueType = value.type()
            require(isAppropriateType(ty, castType, valueType)) {
                "inconsistent types in $name: ty=$ty, castType=$castType, value.type=$valueType"
            }

            return registerUser(Cast(name, ty, castType, value), value)
        }

        private fun isAppropriateType(ty: Type, castType: CastType, valueType: Type): Boolean {
            when (castType) {
                CastType.ZeroExtend,
                CastType.SignExtend,
                CastType.Truncate -> {
                    if (valueType !is ArithmeticType || ty !is ArithmeticType) {
                        return false
                    }

                    return if (castType == CastType.Truncate) {
                        ty.size() < valueType.size()
                    } else {
                        ty.size() >= valueType.size()
                    }
                }
                CastType.Bitcast -> {
                    return valueType == ty
                }
            }
        }

        fun isCorrect(cast: Cast): Boolean {
            return isAppropriateType(cast.type(), cast.castType, cast.value().type())
        }
    }
}