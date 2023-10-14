package ir.instruction

import ir.Type
import ir.Value

class StackAlloc(name: String, ty: Type, private val size: Long):
    ValueInstruction(name, ty.ptr(), arrayOf()) {
    override fun dump(): String {
        return "%$identifier = stackalloc $tp $size"
    }

    fun size(): Long {
        return size
    }

    override fun copy(newUsages: List<Value>): StackAlloc {
        return StackAlloc(identifier, tp.dereference(), size)
    }
}