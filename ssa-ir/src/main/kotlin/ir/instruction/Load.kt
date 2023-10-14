package ir.instruction

import ir.Value

class Load(name: String, ptr: Value):
    ValueInstruction(name, ptr.type().dereference(), arrayOf(ptr)) {
    override fun dump(): String {
        return "%$identifier = load $tp ${operand()}"
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(newUsages: List<Value>): Load {
        return Load(identifier, newUsages[0])
    }
}