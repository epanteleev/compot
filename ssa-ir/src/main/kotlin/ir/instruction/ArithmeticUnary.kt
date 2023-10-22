package ir.instruction

import ir.types.PrimitiveType
import ir.Value
import ir.types.Type


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

class ArithmeticUnary(name: String, tp: PrimitiveType, val op: ArithmeticUnaryOp, value: Value):
    ValueInstruction(name, tp, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $op $tp ${operand()}"
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
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

        return ArithmeticUnary(identifier, type(), op, newUsages[0])
    }
}