package ir.instruction

import ir.instruction.utils.Visitor


class DownStackFrame(callable: Callable): AdjustStackFrame(callable) {

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun dump(): String {
        return "downstackframe [${callable.shortName()}]"
    }

    override fun copy(callable: Callable): AdjustStackFrame {
        return DownStackFrame(callable)
    }
}