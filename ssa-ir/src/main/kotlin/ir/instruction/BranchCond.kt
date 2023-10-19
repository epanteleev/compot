package ir.instruction

import ir.Value
import ir.module.block.Block

class BranchCond(value: Value, onTrue: Block, onFalse: Block):
    TerminateInstruction(arrayOf(value), arrayOf(onTrue, onFalse)) {
    override fun dump(): String {
        return "br u1 ${condition()} label ${onTrue()}, label ${onFalse()} "
    }

    fun condition(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    fun onTrue(): Block {
        assert(targets.size == 2) {
            "size should be 2 in $this instruction"
        }

        return targets[0]
    }

    fun onFalse(): Block {
        assert(targets.size == 2) {
            "size should be 2 in $this instruction"
        }

        return targets[1]
    }

    override fun copy(usages: List<Value>, newTargets: Array<Block>): BranchCond {
        return BranchCond(usages[0], newTargets[0], newTargets[1])
    }

    override fun copy(newUsages: List<Value>): Instruction {
        assert(newUsages.size == 1) {
            "should be"
        }

        return BranchCond(newUsages[0], onTrue(), onFalse())
    }
}