package ir.instruction

import ir.Type
import ir.Value


enum class ArithmeticUnaryOp {
    Neg,
    Not;

    override fun toString(): String {
        val name = when (this) {
            Neg -> "neq"
            Not -> "not"
        }
        return name
    }
}

class ArithmeticUnary(name: String, tp: Type, val op: ArithmeticUnaryOp, value: Value):
    ValueInstruction(name, tp, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $op $tp ${operand()}"
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(newUsages: List<Value>): ArithmeticUnary {
        assert(newUsages.size == 1) {
            "should be"
        }

        return ArithmeticUnary(identifier, tp, op, newUsages[0])
    }
}