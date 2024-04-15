package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class SignExtend private constructor(name: String, toType: SignedIntType, value: Value):
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

    override fun type(): SignedIntType {
        return tp as SignedIntType
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "sext"

        fun make(name: String, toType: SignedIntType, value: Value): SignExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value.type=$valueType"
            }

            return registerUser(SignExtend(name, toType, value), value)
        }

        private fun isAppropriateType(toType: SignedIntType, valueType: Type): Boolean {
            if (valueType !is SignedIntType) {
                return false
            }

            return toType.size() > valueType.size()
        }

        fun typeCheck(sext: SignExtend): Boolean {
            return isAppropriateType(sext.type(), sext.value().type())
        }
    }
}