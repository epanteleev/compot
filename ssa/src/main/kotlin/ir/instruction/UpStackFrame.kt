package ir.instruction

import ir.instruction.utils.Visitor


class UpStackFrame(callable: Callable): AdjustStackFrame(callable) {
    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        return "upstackframe [${callable.shortName()}]"
    }
}