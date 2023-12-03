package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.*
import ir.module.block.Block

class BranchCond private constructor(value: Value, onTrue: Block, onFalse: Block) :
    TerminateInstruction(arrayOf(value), arrayOf(onTrue, onFalse)) {
    override fun dump(): String {
        return "br u1 ${condition()} label %${onTrue()}, label %${onFalse()} "
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

    override fun copy(newUsages: List<Value>, newTargets: Array<Block>): BranchCond {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(newUsages[0], newTargets[0], newTargets[1])
    }

    override fun copy(newUsages: List<Value>): Instruction {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(newUsages[0], onTrue(), onFalse())
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(value: Value, onTrue: Block, onFalse: Block): BranchCond {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "should be boolean type, but value.type=$valueType"
            }

            return registerUser(BranchCond(value, onTrue, onFalse), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is BooleanType
        }

        fun isCorrect(branchCond: BranchCond): Boolean {
            return isAppropriateType(branchCond.condition().type())
        }
    }
}