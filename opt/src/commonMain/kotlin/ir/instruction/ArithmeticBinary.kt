package ir.instruction

import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.types.ArithmeticType


abstract class ArithmeticBinary(id: Identity, owner: Block, protected val tp: ArithmeticType, a: Value, b: Value) :
    ValueInstruction(id, owner, arrayOf(a, b)) {

    fun lhs(): Value {
        check()
        return operands[LHS]
    }

    fun lhs(newLhs: Value) {
        update(LHS, newLhs)
    }

    fun rhs(): Value {
        check()
        return operands[RHS]
    }

    fun rhs(newRhs: Value) {
        update(RHS, newRhs)
    }

    private fun check() {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }
    }

    companion object {
        private const val LHS = 0
        private const val RHS = 1
    }
}