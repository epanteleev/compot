package ir.instruction

import ir.module.block.Block


abstract class AdjustStackFrame(id: Identity, owner: Block, protected open val callable: Callable): Instruction(id, owner, arrayOf()) {
    fun call(): Callable = callable
}