package ir.instruction

import ir.value.Value
import ir.module.block.Block


sealed class TerminateInstruction(id: Identity, owner: Block, usages: Array<Value>, val targets: Array<Block>):
    Instruction(id, owner, usages) {
    fun targets(): Array<Block> = targets

    fun target(newBB: Block, old: Block) = owner.cf {
        val index = owner.successors.indexOf(old)
        owner.successors[index] = newBB

        newBB.predecessors.add(owner)
        old.predecessors.remove(owner)

        targets[index] = newBB

        owner.updatePhi(old, newBB)
    }
}