package ir.instruction

import common.assertion
import ir.Value
import ir.types.Type
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.ArithmeticType


class Neg private constructor(id: Identity, owner: Block, tp: ArithmeticType, value: Value):
    ArithmeticUnary(id, owner, tp, value) {
    override fun dump(): String {
        return "%${name()} = $NAME $tp ${operand()}"
    }

    override fun type(): ArithmeticType {
        return tp as ArithmeticType
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
        const val NAME = "neg"

        fun make(id: Identity, owner: Block, type: ArithmeticType, value: Value): Neg {
            val valueType = value.type()
            require(isAppropriateTypes(type, valueType)) {
                "should be the same type, but type=$type, value=$value:$valueType"
            }

            return registerUser(Neg(id, owner, type, value), value)
        }

        private fun isAppropriateTypes(tp: ArithmeticType, argType: Type): Boolean {
            return tp == argType
        }

        fun typeCheck(unary: Neg): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}