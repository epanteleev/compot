package ir.instruction

import ir.Value

class Store(pointer: Value, value: Value):
    Instruction(pointer.type(), arrayOf(pointer, value)) {
    override fun dump(): String {
        return "store $tp ${pointer()}, ${value()}"
    }

    fun pointer(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun value(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun copy(newUsages: List<Value>): Store {
        return Store(newUsages[0], newUsages[1])
    }

    override fun hashCode(): Int {
        return pointer().hashCode() and value().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Store
        return pointer() == other.pointer() && value() == other.value()
    }
}