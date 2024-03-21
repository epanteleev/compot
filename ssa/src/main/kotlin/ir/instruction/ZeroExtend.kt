package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class ZeroExtend private constructor(name: String, toType: UnsignedIntType, value: Value):
    ValueInstruction(name, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): UnsignedIntType {
        return tp as UnsignedIntType
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "zext"

        fun make(name: String, toType: UnsignedIntType, value: Value): ZeroExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value.type=$valueType"
            }

            return registerUser(ZeroExtend(name, toType, value), value)
        }

        private fun isAppropriateType(toType: UnsignedIntType, valueType: Type): Boolean {
            if (valueType !is UnsignedIntType) {
                return false
            }

            return toType.size() > valueType.size()
        }

        fun isCorrect(zext: ZeroExtend): Boolean {
            return isAppropriateType(zext.type(), zext.value().type())
        }
    }
}