package ir.instruction

import ir.*
import ir.types.PointerType
import ir.types.Type

class GetElementPtr(name: String, tp: Type, source: Value, index: Value):
    ValueInstruction(name, tp, arrayOf(source, index)) {
    override fun dump(): String {
        return "%$identifier = gep $tp ${source()}, ${index().type()} ${index()}"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    fun source(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun copy(newUsages: List<Value>): GetElementPtr {
        return GetElementPtr(identifier, type(), newUsages[0], newUsages[1])
    }
}