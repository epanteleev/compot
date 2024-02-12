package ir.instruction

import ir.instruction.utils.Visitor


class UpStackFrame(callable: Callable): AdjustStackFrame(callable) {
    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun dump(): String {
        return "upstackframe [${callable.shortName()}]"
    }

    override fun copy(callable: Callable): AdjustStackFrame {
        return UpStackFrame(callable)
    }
}