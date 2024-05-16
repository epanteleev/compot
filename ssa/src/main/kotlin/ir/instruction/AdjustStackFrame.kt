package ir.instruction

abstract class AdjustStackFrame(protected open val callable: Callable): Instruction(arrayOf()) {
    fun call(): Callable = callable

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdjustStackFrame
        return other.callable == callable
    }

    override fun hashCode(): Int {
        return callable.hashCode()
    }
}