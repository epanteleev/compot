package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class DownStackFrame private constructor(id: Identity, owner: Block, callable: Callable): AdjustStackFrame(id, owner, callable) {
    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        return "downstackframe [${callable.shortName()}]"
    }

    companion object {
        fun dsf(callable: Callable): InstBuilder<DownStackFrame> = { id: Identity, owner: Block ->
            DownStackFrame(id, owner, callable)
        }
    }
}