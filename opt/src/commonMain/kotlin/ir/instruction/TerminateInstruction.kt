package ir.instruction

import ir.value.Value
import ir.module.block.Block


//TODO is it interface?
abstract class TerminateInstruction(id: Identity, owner: Block, usages: Array<Value>, val targets: Array<Block>):
    Instruction(id, owner, usages) {
    fun targets(): Array<Block> = targets

    // DO NOT USE THIS METHOD DIRECTLY
    internal fun updateTargets(transform: (Block) -> Block) {
        for ((i, v) in targets.withIndex()) {
            targets[i] = transform(v)
        }
    }
}