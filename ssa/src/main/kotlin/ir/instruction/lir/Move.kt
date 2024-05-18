package ir.instruction.lir

import ir.Value
import ir.instruction.Generate
import ir.instruction.Instruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Move private constructor(owner: Block, destination: Value, source: Value):
    Instruction(owner, arrayOf(destination, source)) {

    override fun dump(): String {
        val fromValue = source()
        return "$NAME ${fromValue.type()} ${destination()} $fromValue"
    }

    fun source(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun destination(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Move
        return source() == other.source() && destination() == other.destination()
    }

    override fun hashCode(): Int {
        return source().type().hashCode() xor destination().type().hashCode()
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "move"

        fun make(owner: Block, dst: Generate, src: Value): Move {
            require(isAppropriateType(dst, src)) {
                "inconsistent types: toValue=$dst:${dst.type()}, fromValue=$src:${src.type()}"
            }

            return registerUser(Move(owner, dst, src), dst, src)
        }

        fun typeCheck(copy: Move): Boolean {
            return isAppropriateType(copy.destination(), copy.source())
        }

        private fun isAppropriateType(toValue: Value, fromValue: Value): Boolean {
            return toValue.type() == fromValue.type()
        }
    }
}