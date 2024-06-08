package ir.instruction

import common.assertion
import ir.Value
import ir.types.Type
import ir.types.IntegerType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


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

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "not"

        fun make(id: Identity, owner: Block, type: IntegerType, value: Value): Not {
            val valueType = value.type()
            require(isAppropriateTypes(type, valueType)) {
                "inconsistent type in '$id', but type=$type, value=$value:$valueType"
            }

            return registerUser(Not(id, owner, type, value), value)
        }

        private fun isAppropriateTypes(tp: IntegerType, argType: Type): Boolean {
            return tp == argType
        }

        fun typeCheck(unary: Not): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}