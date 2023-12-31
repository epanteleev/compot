package ir.instruction

import ir.Value
import ir.types.Type
import ir.instruction.utils.Visitor
import ir.types.ArithmeticType


class Neg private constructor(name: String, tp: ArithmeticType, value: Value):
    ArithmeticUnary(name, tp, value) {
    override fun dump(): String {
        return "%$identifier = $NAME $tp ${operand()}"
    }

    override fun type(): ArithmeticType {
        return tp as ArithmeticType
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(newUsages: List<Value>): Neg {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, type(), newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        const val NAME = "neg"

        fun make(name: String, tp: ArithmeticType, value: Value): Neg {
            val valueType = value.type()
            require(isAppropriateTypes(tp, valueType)) {
                "should be the same type, but tp=$tp, value.type=$valueType"
            }

            return registerUser(Neg(name, tp, value), value)
        }

        private fun isAppropriateTypes(tp: ArithmeticType, argType: Type): Boolean {
            return tp == argType
        }

        fun isCorrect(unary: Neg): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}