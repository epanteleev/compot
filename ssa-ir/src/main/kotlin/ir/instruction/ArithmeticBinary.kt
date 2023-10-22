package ir.instruction

import ir.types.PrimitiveType
import ir.Value
import ir.types.Type

enum class ArithmeticBinaryOp {
    Add,
    Sub,
    Mul,
    Mod,
    Div,
    Shr,
    Shl,
    And,
    Or,
    Xor;

    override fun toString(): String {
        val name = when (this) {
            Add -> "add"
            Sub -> "sub"
            Mul -> "mul"
            Mod -> "mod"
            Div -> "div"
            Shr -> "shr"
            Shl -> "shl"
            And -> "and"
            Or  -> "or"
            Xor -> "xor"
        }
        return name
    }
}

class ArithmeticBinary(name: String, tp: PrimitiveType, a: Value, val op: ArithmeticBinaryOp, b: Value):
    ValueInstruction(name, tp, arrayOf(a, b)) {
    override fun dump(): String {
        return "%$identifier = $op $tp ${first()}, ${second()}"
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    fun first(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun second(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun copy(newUsages: List<Value>): ArithmeticBinary {
        return ArithmeticBinary(identifier, type(), newUsages[0], op, newUsages[1])
    }
}