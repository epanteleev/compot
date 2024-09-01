package ir.instruction

import common.assertion
import ir.value.Value
import ir.module.block.Block
import ir.types.ArithmeticType


abstract class ArithmeticBinary(id: Identity, owner: Block, tp: ArithmeticType, a: Value, b: Value) :
    ValueInstruction(id, owner, tp, arrayOf(a, b)) {

    fun first(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[FIRST]
    }

    fun second(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[SECOND]
    }

    companion object {
        const val FIRST = 0
        const val SECOND = 1
    }
}