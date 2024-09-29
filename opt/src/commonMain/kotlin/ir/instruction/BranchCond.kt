package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class BranchCond private constructor(id: Identity, owner: Block, value: Value, onTrue: Block, onFalse: Block) :
    TerminateInstruction(id, owner, arrayOf(value), arrayOf(onTrue, onFalse)) {
    override fun dump(): String {
        return "br u1 ${condition()} label %${onTrue()}, label %${onFalse()} "
    }

    fun condition(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    fun onTrue(): Block {
        assertion(targets.size == 2) {
            "size should be 2 in $this instruction"
        }

        return targets[0]
    }

    fun onFalse(): Block {
        assertion(targets.size == 2) {
            "size should be 2 in $this instruction"
        }

        return targets[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(id: Identity, owner: Block, value: Value, onTrue: Block, onFalse: Block): BranchCond {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "should be boolean type, but value=$value:$valueType"
            }

            return registerUser(BranchCond(id, owner, value, onTrue, onFalse), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is FlagType
        }

        fun typeCheck(branchCond: BranchCond): Boolean {
            return isAppropriateType(branchCond.condition().type())
        }
    }
}