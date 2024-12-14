package ir.instruction.lir

import common.assertion
import ir.global.GlobalValue
import ir.value.Value
import ir.instruction.Identity
import ir.instruction.InstBuilder
import ir.instruction.Instruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.value.UsableValue


class Move private constructor(id: Identity, owner: Block, destination: Value, source: Value):
    Instruction(id, owner, arrayOf(destination, source)) {

    override fun dump(): String {
        val fromValue = source()
        return "$NAME ${fromValue.type()} ${destination()} $fromValue"
    }

    fun source(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun destination(): UsableValue {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0] as UsableValue
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "move"

        fun move(dst: UsableValue, src: Value): InstBuilder<Move> = { id: Identity, owner: Block ->
            make(id, owner, dst, src)
        }

        private fun make(id: Identity, owner: Block, dst: UsableValue, src: Value): Move {
            require(isAppropriateType(dst, src)) {
                "inconsistent types: toValue=$dst:${dst.type()}, fromValue=$src:${src.type()}"
            }

            return registerUser(Move(id, owner, dst, src), dst, src)
        }

        fun typeCheck(copy: Move): Boolean {
            return isAppropriateType(copy.destination(), copy.source())
        }

        private fun isAppropriateType(toValue: UsableValue, fromValue: Value): Boolean {
            return (toValue is Generate || toValue is GlobalValue) && toValue.type() == fromValue.type()
        }
    }
}