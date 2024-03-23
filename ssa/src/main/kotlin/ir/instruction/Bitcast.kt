package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class Bitcast private constructor(name: String, toType: Type, value: Value):
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

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "bitcast"

        fun make(name: String, toType: PrimitiveType, value: Value): Bitcast {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value.type=$valueType"
            }

            return registerUser(Bitcast(name, toType, value), value)
        }

        private fun isAppropriateType(toType: Type, valueType: Type): Boolean {
            return valueType == toType && toType !is FloatingPointType
        }

        fun isCorrect(bitcast: Bitcast): Boolean {
            return isAppropriateType(bitcast.type(), bitcast.value().type())
        }
    }
}