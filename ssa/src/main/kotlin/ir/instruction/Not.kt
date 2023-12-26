package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.IntegerType
import ir.instruction.utils.Visitor


class Not private constructor(name: String, tp: IntegerType, value: Value):
    ArithmeticUnary(name, tp, value) {
    override fun dump(): String {
        return "%$identifier = not $tp ${operand()}"
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

    override fun copy(newUsages: List<Value>): Not {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, type(), newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, tp: IntegerType, value: Value): Not {
            val valueType = value.type()
            require(isAppropriateTypes(tp, valueType)) {
                "should be the same type, but tp=$tp, value.type=$valueType"
            }

            return registerUser(Not(name, tp, value), value)
        }

        private fun isAppropriateTypes(tp: IntegerType, argType: Type): Boolean {
            return tp == argType
        }

        fun isCorrect(unary: Not): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}