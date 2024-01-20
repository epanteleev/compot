package ir.instruction

import ir.Value
import ir.module.block.Block
import ir.instruction.utils.Visitor


class Branch private constructor(target: Block): TerminateInstruction(arrayOf(), arrayOf(target)) {
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
        return make(target())
    }

    override fun copy(usages: List<Value>, newTargets: Array<Block>): Branch {
        return make(newTargets[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(target: Block): Branch {
            return Branch(target)
        }
    }
}