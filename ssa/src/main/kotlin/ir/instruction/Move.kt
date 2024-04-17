package ir.instruction

import ir.Value
import ir.instruction.utils.IRInstructionVisitor


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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Move
        return fromValue() == other.fromValue() && toValue() == other.toValue()
    }

    override fun hashCode(): Int {
        return fromValue().type().hashCode() xor toValue().type().hashCode()
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "move"

        fun make(toValue: Generate, fromValue: Value): Move {
            require(isAppropriateType(toValue, fromValue)) {
                "inconsistent types: toValue=$toValue:${toValue.type()}, fromValue=$fromValue:${fromValue.type()}"
            }

            return registerUser(Move(toValue, fromValue), toValue, fromValue)
        }

        fun typeCheck(copy: Move): Boolean {
            return isAppropriateType(copy.toValue(), copy.fromValue())
        }

        private fun isAppropriateType(toValue: Value, fromValue: Value): Boolean {
            if (toValue is Generate || fromValue is Generate) {
                return fromValue.type() == toValue.type()
            }

            return false
        }
    }
}