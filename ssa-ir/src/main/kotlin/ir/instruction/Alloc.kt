package ir.instruction

import ir.Type
import ir.Value

class Alloc(name: String, ty: Type, private val size: Long):
    ValueInstruction(name, ty.ptr(), arrayOf()) {
    override fun dump(): String {
        return "%$identifier = alloc $tp $size"
    }

    fun size(): Long {
        return size
    }

    override fun copy(newUsages: List<Value>): Alloc {
        return Alloc(identifier, tp.dereference(), size)
    }
}