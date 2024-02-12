package ir.instruction

import ir.Value

abstract class AdjustStackFrame(protected open val callable: Callable): Instruction(arrayOf()) {
    fun call(): Callable = callable

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownStackFrame
        return other.callable == callable
    }

    override fun hashCode(): Int {
        return callable.hashCode()
    }

    override fun copy(newUsages: List<Value>): Instruction = TODO()
    abstract fun copy(callable: Callable): AdjustStackFrame
}