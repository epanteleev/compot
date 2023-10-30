package ir.instruction

import ir.Value
import ir.types.PointerType
import ir.types.PrimitiveType
import ir.types.Type

class Load(name: String, ptr: Value):
    ValueInstruction(name, (ptr.type() as PointerType).dereference(), arrayOf(ptr)) {
    override fun dump(): String {
        return "%$identifier = load $tp ${operand()}"
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun copy(newUsages: List<Value>): Load {
        return Load(identifier, newUsages[0])
    }
}