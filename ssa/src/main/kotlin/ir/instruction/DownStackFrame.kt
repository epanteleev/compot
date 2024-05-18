package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class DownStackFrame(owner: Block, callable: Callable): AdjustStackFrame(owner, callable) {

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        return "downstackframe [${callable.shortName()}]"
    }
}