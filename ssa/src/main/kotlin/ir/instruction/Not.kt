package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.IntegerType
import ir.instruction.utils.Visitor


class Not private constructor(name: String, tp: IntegerType, value: Value):
    ArithmeticUnary(name, tp, value) {
    override fun dump(): String {
        return "%$identifier = $NAME $tp ${operand()}"
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

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "not"

        fun make(name: String, type: IntegerType, value: Value): Not {
            val valueType = value.type()
            require(isAppropriateTypes(type, valueType)) {
                "inconsistent type in '$name', but type=$type, value=$value:$valueType"
            }

            return registerUser(Not(name, type, value), value)
        }

        private fun isAppropriateTypes(tp: IntegerType, argType: Type): Boolean {
            return tp == argType
        }

        fun typeCheck(unary: Not): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}