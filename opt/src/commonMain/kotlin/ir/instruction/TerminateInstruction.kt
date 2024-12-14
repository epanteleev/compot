package ir.instruction

import ir.value.Value
import ir.module.block.Block


sealed class TerminateInstruction(id: Identity, owner: Block, usages: Array<Value>, val targets: Array<Block>):
    Instruction(id, owner, usages) {
    fun targets(): Array<Block> = targets

    // DO NOT USE THIS METHOD DIRECTLY
    internal fun updateTarget(newBB: Block, index: Int) {
        targets[index] = newBB
    }
}