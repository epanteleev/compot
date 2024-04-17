package ir.instruction

import ir.instruction.utils.IRInstructionVisitor


class UpStackFrame(callable: Callable): AdjustStackFrame(callable) {
    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        return "upstackframe [${callable.shortName()}]"
    }
}