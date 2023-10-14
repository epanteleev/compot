package ir.instruction

import ir.Type
import ir.Value

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