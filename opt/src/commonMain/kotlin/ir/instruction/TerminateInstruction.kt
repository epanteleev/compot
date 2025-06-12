package ir.instruction

import ir.value.Value
import ir.module.block.Block


sealed class TerminateInstruction(id: Identity, owner: Block, usages: Array<Value>, val targets: Array<Block>):
    Instruction(id, owner, usages) {
    fun targets(): Array<Block> = targets

    fun target(newBB: Block, old: Block) = owner.cf {
        val index = targets.indexOf(old)
        targets[index] = newBB

        newBB.predecessors.add(owner)
        old.predecessors.remove(owner)

        owner.updatePhi(old, newBB)
    }
}