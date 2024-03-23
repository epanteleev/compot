package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class FloatToSigned private constructor(name: String, toType: SignedIntType, value: Value):
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
        const val NAME = "fptosi"

        fun make(name: String, toType: SignedIntType, value: Value): FloatToSigned {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "inconsistent types in $name: ty=$toType, value.type=$valueType"
            }

            return registerUser(FloatToSigned(name, toType, value), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is FloatingPointType
        }

        fun isCorrect(fpext: FloatToSigned): Boolean {
            return isAppropriateType(fpext.value().type())
        }
    }
}