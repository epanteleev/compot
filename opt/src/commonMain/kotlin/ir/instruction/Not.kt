package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.Type
import ir.types.IntegerType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.asType


class Not private constructor(id: Identity, owner: Block, tp: IntegerType, value: Value):
    ArithmeticUnary(id, owner, tp, value) {
    override fun dump(): String {
        return "%${name()} = $NAME $tp ${operand()}"
    }

    override fun type(): IntegerType {
        return tp as IntegerType
    }

    fun operand(): Value {
        assertion(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "not"

        fun not(value: Value): InstBuilder<Not> = { id: Identity, owner: Block ->
            make(id, owner, value)
        }

        private fun make(id: Identity, owner: Block, value: Value): Not {
            val valueType = value.type()
            return registerUser(Not(id, owner, valueType.asType(), value), value)
        }


        fun typeCheck(unary: Not): Boolean {
            return true
        }
    }
}