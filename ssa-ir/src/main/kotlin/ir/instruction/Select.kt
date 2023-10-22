package ir.instruction

import ir.*
import ir.types.Type

class Select(name: String, ty: Type, cond: Value, onTrue: Value, onFalse: Value) :
    ValueInstruction(name, ty, arrayOf(cond, onTrue, onFalse)) {
    override fun dump(): String {
        return "%$identifier = select $tp ${condition()} ${onTrue()}, ${onFalse()}"
    }

    fun condition(): Value {
        assert(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[0]
    }

    fun onTrue(): Value {
        assert(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[1]
    }

    fun onFalse(): Value {
        assert(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[2]
    }

    override fun copy(newUsages: List<Value>): Select {
        return Select(identifier, tp, newUsages[0], newUsages[1], newUsages[2])
    }
}