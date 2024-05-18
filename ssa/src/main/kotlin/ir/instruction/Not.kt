package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.IntegerType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Not private constructor(name: String, owner: Block, tp: IntegerType, value: Value):
    ArithmeticUnary(name, owner, tp, value) {
    override fun dump(): String {
        return "%$id = $NAME $tp ${operand()}"
    }

    override fun type(): IntegerType {
        return tp as IntegerType
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "not"

        fun make(name: String, owner: Block, type: IntegerType, value: Value): Not {
            val valueType = value.type()
            require(isAppropriateTypes(type, valueType)) {
                "inconsistent type in '$name', but type=$type, value=$value:$valueType"
            }

            return registerUser(Not(name, owner, type, value), value)
        }

        private fun isAppropriateTypes(tp: IntegerType, argType: Type): Boolean {
            return tp == argType
        }

        fun typeCheck(unary: Not): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}