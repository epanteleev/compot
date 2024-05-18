package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class ReturnVoid private constructor(owner: Block): Return(owner, arrayOf()) {
    override fun dump(): String = "ret void"

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(owner: Block): ReturnVoid = ReturnVoid(owner)
    }
}