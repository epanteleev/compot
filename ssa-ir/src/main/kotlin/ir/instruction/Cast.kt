package ir.instruction

import ir.Value
import ir.types.Type

enum class CastType {
    ZeroExtend,
    SignExtend,
    Truncate,
    Bitcast;

    override fun toString(): String {
        val name = when (this) {
            ZeroExtend -> "zext"
            SignExtend -> "sext"
            Truncate   -> "trunc"
            Bitcast    -> "bitcast"
        }
        return name
    }
}

class Cast(name: String, ty: Type, val castType: CastType, value: Value):
    ValueInstruction(name, ty, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $castType $tp ${value()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(newUsages: List<Value>): Cast {
        return Cast(identifier, tp, castType, newUsages[0])
    }
}