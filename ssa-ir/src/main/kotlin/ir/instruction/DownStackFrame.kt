package ir.instruction

import ir.*
import ir.instruction.utils.Visitor
import ir.types.Type

class DownStackFrame(private val callable: Callable):
    Instruction(Type.UNDEF, arrayOf()) {
    fun call(): Callable {
        return callable
    }

    override fun copy(newUsages: List<Value>): Instruction {
        return UpStackFrame(callable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownStackFrame
        return other.callable == callable
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun hashCode(): Int {
        return callable.hashCode()
    }

    override fun dump(): String {
        return "downstackframe [${callable.shortName()}]"
    }
}