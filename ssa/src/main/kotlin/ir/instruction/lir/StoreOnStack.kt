package ir.instruction.lir

import ir.Value
import ir.instruction.Instruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.AggregateType
import ir.types.ArithmeticType
import ir.types.PrimitiveType

class StoreOnStack private constructor(owner: Block, destination: Value, index: Value, source: Value):
    Instruction(owner, arrayOf(destination, index, source)) {

    override fun dump(): String {
        val fromValue = source()
        return "$NAME ${fromValue.type()} ${destination()}: ${index().type()} ${index()} $fromValue"
    }

    fun source(): Value {
        assert(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[2]
    }

    fun destination(): Value {
        assert(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Value {
        assert(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
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
        const val NAME = "movst"

        fun make(owner: Block, dst: Value, index: Value, src: Value): StoreOnStack {
            require(isAppropriateType(dst, index, src)) {
                "inconsistent types: toValue=$dst:${dst.type()}, base=$src:${src.type()}"
            }

            return registerUser(StoreOnStack(owner, dst, index, src), dst, index, src)
        }

        fun typeCheck(copy: StoreOnStack): Boolean {
            return isAppropriateType(copy.destination(), copy.index(), copy.source())
        }

        private fun isAppropriateType(toValue: Value, index: Value, fromValue: Value): Boolean {
            return toValue.type() is AggregateType && index.type() is ArithmeticType && fromValue.type() is PrimitiveType
        }
    }
}