package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.ArithmeticType
import ir.types.Type

enum class ArithmeticUnaryOp {
    Neg {
        override fun toString(): String {
            return "neg"
        }
    },
    Not {
        override fun toString(): String {
            return "not"
        }
    };
}

class ArithmeticUnary private constructor(name: String, tp: ArithmeticType, val op: ArithmeticUnaryOp, value: Value):
    ValueInstruction(name, tp, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $op $tp ${operand()}"
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

    override fun copy(newUsages: List<Value>): ArithmeticUnary {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, type(), op, newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, tp: ArithmeticType, op: ArithmeticUnaryOp, value: Value): ArithmeticUnary {
            val valueType = value.type()
            require(isAppropriateTypes(tp, valueType)) {
                "should be the same type, but tp=$tp, value.type=$valueType"
            }

            return registerUser(ArithmeticUnary(name, tp, op, value), value)
        }

        private fun isAppropriateTypes(tp: ArithmeticType, argType: Type): Boolean {
            return tp == argType
        }

        fun isCorrect(unary: ArithmeticUnary): Boolean {
            return isAppropriateTypes(unary.type(), unary.operand().type())
        }
    }
}