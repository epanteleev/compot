package ir.instruction

import ir.Value
import ir.types.*

class Alloc(name: String, ty: Type):
    ValueInstruction(name, ty.ptr(), arrayOf()) {
    override fun dump(): String {
        return "%$identifier = alloc $tp"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun copy(newUsages: List<Value>): Alloc {
        return Alloc(identifier, type().dereference())
    }
}