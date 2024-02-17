package ir.instruction

import ir.Value
import ir.types.PrimitiveType
import ir.instruction.utils.Visitor


class Move private constructor(toValue: Generate, fromValue: Value):
    Instruction(arrayOf(toValue, fromValue)) {

    override fun dump(): String {
        val fromValue = fromValue()
        return "$NAME ${fromValue.type()} ${toValue()} $fromValue"
    }

    fun fromValue(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun toValue(): Generate {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0] as Generate
    }

    override fun copy(newUsages: List<Value>): Move {
        assert(newUsages.size == 2) {
            "should be, but newUsages=$newUsages"
        }

        return make(newUsages[0] as Generate, newUsages[1])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Move
        return fromValue() == other.fromValue() && toValue() == other.toValue()
    }

    override fun hashCode(): Int {
        return fromValue().type().hashCode() xor toValue().type().hashCode()
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        const val NAME = "move"

        fun make(toValue: Generate, fromValue: Value): Move {
            require(isAppropriateType(toValue, fromValue)) {
                "inconsistent types: toValue=$toValue, fromValue=$fromValue"
            }

            return registerUser(Move(toValue, fromValue), toValue, fromValue)
        }

        fun isCorrect(copy: Move): Boolean {
            return isAppropriateType(copy.toValue(), copy.fromValue())
        }

        private fun isAppropriateType(toValue: Value, fromValue: Value): Boolean {
            if (toValue is Generate || fromValue is Generate) {
                return fromValue.type() is PrimitiveType
            }

            return false
        }
    }
}