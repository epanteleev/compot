package ir.instruction.lir

import common.assertion
import ir.Definitions.WORD_SIZE
import ir.value.Value
import ir.instruction.Identity
import ir.instruction.Instruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.IntegerType
import ir.types.NonTrivialType
import ir.types.PointerType
import ir.types.PrimitiveType


class MoveByIndex private constructor(id: Identity, owner: Block, destination: Value, index: Value, source: Value):
    Instruction(id, owner, arrayOf(destination, index, source)) {

    override fun dump(): String {
        val fromValue = index()
        return "$NAME ${fromValue.type()} ${destination()}:${source()} $fromValue"
    }

    fun index(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun destination(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun source(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[2]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Move
        return index() == other.source() && destination() == other.destination()
    }

    override fun hashCode(): Int {
        return index().type().hashCode() xor destination().type().hashCode()
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "move"

        fun make(id: Identity, owner: Block, dst: Value, index: Value, src: Value): MoveByIndex {
            require(isAppropriateType(dst, index, src)) {
                "inconsistent types: dst=$dst:${dst.type()}, index=$index:${index.type()}, src=$src:${src.type()}"
            }

            return registerUser(MoveByIndex(id, owner, dst, index, src), dst, index, src)
        }

        fun typeCheck(copy: MoveByIndex): Boolean {
            return isAppropriateType(copy.destination(), copy.index(), copy.source())
        }

        private fun isAppropriateType(toValue: Value, index: Value, src: Value): Boolean {
            if (toValue.type() !is PointerType) {
                return false
            }
            if (src.type() !is PrimitiveType) {
                return false
            }
            val type = index.type()
            if (type !is IntegerType) {
                return false
            }
            return true
        }
    }
}