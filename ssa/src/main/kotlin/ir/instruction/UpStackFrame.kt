package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor

class UpStackFrame(private val callable: Callable): Instruction(arrayOf()) {
    fun call(): Callable {
        return callable
    }

    override fun copy(newUsages: List<Value>): UpStackFrame {
        return UpStackFrame(callable)
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpStackFrame
        return other.callable == callable
    }

    override fun hashCode(): Int {
        return callable.hashCode()
    }

    override fun dump(): String {
        return "upstackframe [${callable.shortName()}]"
    }
}