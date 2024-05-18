package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class UpStackFrame(id: Identity, owner: Block, callable: Callable): AdjustStackFrame(id, owner, callable) {
    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        return "upstackframe [${callable.shortName()}]"
    }
}