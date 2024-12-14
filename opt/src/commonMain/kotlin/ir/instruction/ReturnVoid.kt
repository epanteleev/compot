package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class ReturnVoid private constructor(id: Identity, owner: Block): Return(id, owner, arrayOf()) {
    override fun dump(): String = "ret void"

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun ret(): InstBuilder<ReturnVoid> = { id: Identity, owner: Block ->
            ReturnVoid(id, owner)
        }
    }
}