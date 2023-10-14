package ir.instruction

import ir.Value

class Copy(name: String, origin: Value):
    ValueInstruction(name, origin.type(), arrayOf(origin)) {
    override fun dump(): String {
        return "%$identifier = copy $tp ${origin()}"
    }

    fun origin(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(newUsages: List<Value>): Copy {
        return Copy(identifier, newUsages[0])
    }
}