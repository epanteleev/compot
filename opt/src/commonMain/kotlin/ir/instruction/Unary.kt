package ir.instruction

import common.assertion
import ir.value.Value
import ir.module.block.Block
import ir.types.PrimitiveType


sealed class Unary(id: Identity, owner: Block, protected val tp: PrimitiveType, value: Value) :
    ValueInstruction(id, owner, arrayOf(value)) {
    fun operand(): Value {
        assertion(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun operand(newOperand: Value) {
        update(0, newOperand)
    }
}