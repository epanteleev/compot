package ir.instruction.lir

import common.assertion
import ir.value.Value
import ir.types.*
import ir.module.block.Block
import ir.instruction.Identity
import ir.instruction.Instruction
import ir.instruction.utils.IRInstructionVisitor


class StoreOnStack private constructor(id: Identity, owner: Block, destination: Value, index: Value, source: Value):
    Instruction(id, owner, arrayOf(destination, index, source)) {

    override fun dump(): String {
        val fromValue = source()
        return "$NAME ${fromValue.type()} ${destination()}: ${index().type()} ${index()} $fromValue"
    }

    fun source(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[2]
    }

    fun destination(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "movst"

        fun make(id: Identity, owner: Block, dst: Value, index: Value, src: Value): StoreOnStack {
            require(isAppropriateType(dst, index, src)) {
                "inconsistent types: toValue=$dst:${dst.type()}, base=$src:${src.type()}"
            }

            return registerUser(StoreOnStack(id, owner, dst, index, src), dst, index, src)
        }

        fun typeCheck(copy: StoreOnStack): Boolean {
            return isAppropriateType(copy.destination(), copy.index(), copy.source())
        }

        private fun isAppropriateType(toValue: Value, index: Value, fromValue: Value): Boolean {
            return toValue.type() is AggregateType && index.type() is ArithmeticType && fromValue.type() is PrimitiveType
        }
    }
}