package ir.instruction

import ir.module.block.Block


abstract class AdjustStackFrame(id: Identity, owner: Block, protected open val callable: Callable): Instruction(id, owner, arrayOf()) {
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