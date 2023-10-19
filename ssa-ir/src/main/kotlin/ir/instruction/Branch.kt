package ir.instruction

import ir.Value
import ir.module.block.Block


class Branch(target: Block): TerminateInstruction(arrayOf(), arrayOf(target)) {
    override fun dump(): String {
        return "br label ${target()}"
    }

    fun target(): Block {
        assert(targets.size == 1) {
            "size should be 1 in $this instruction"
        }

        return targets[0]
    }

    override fun copy(newUsages: List<Value>): Instruction {
        return this
    }

    override fun copy(usages: List<Value>, newTargets: Array<Block>): Branch {
        return Branch(newTargets[0])
    }
}