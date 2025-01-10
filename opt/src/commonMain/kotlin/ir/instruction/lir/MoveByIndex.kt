package ir.instruction.lir

import common.assertion
import ir.Definitions.QWORD_SIZE
import ir.types.*
import ir.value.Value
import ir.instruction.Identity
import ir.instruction.InstBuilder
import ir.instruction.Instruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class MoveByIndex private constructor(id: Identity, owner: Block, destination: Value, index: Value, source: Value):
    Instruction(id, owner, arrayOf(destination, index, source)) {

    override fun dump(): String {
        return "$NAME ${source().type()} ${destination()}: ${index()}, ${source()}"
    }

    private fun getOperand(idx: Int): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[idx]
    }

    fun index(): Value = getOperand(1)

    fun destination(): Value = getOperand(0)

    fun source(): Value = getOperand(2)

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "move"

        fun move(dst: Value, index: Value, src: Value): InstBuilder<MoveByIndex> = { id: Identity, owner: Block ->
            make(id, owner, dst, index, src)
        }

        private fun make(id: Identity, owner: Block, dst: Value, index: Value, src: Value): MoveByIndex {
            require(isAppropriateType(dst, index, src)) {
                "inconsistent types: dst=$dst:${dst.type()}, index=$index:${index.type()}, src=$src:${src.type()}"
            }

            return registerUser(MoveByIndex(id, owner, dst, index, src), dst, index, src)
        }

        fun typeCheck(copy: MoveByIndex): Boolean {
            return isAppropriateType(copy.destination(), copy.index(), copy.source())
        }

        private fun isAppropriateType(toValue: Value, index: Value, src: Value): Boolean {
            if (toValue.type() !is PtrType) {
                return false
            }
            if (src.type() !is PrimitiveType) {
                return false
            }
            val type = index.type()
            if (type !is IntegerType) {
                return false
            }
            return type.sizeOf() == QWORD_SIZE
        }
    }
}