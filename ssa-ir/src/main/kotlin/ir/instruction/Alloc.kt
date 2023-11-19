package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.*

class Alloc private constructor(name: String, ty: Type):
    ValueInstruction(name, ty.ptr(), arrayOf()) {
    override fun dump(): String {
        return "%$identifier = alloc ${type().dereference()}"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun copy(newUsages: List<Value>): Alloc {
        return make(identifier, type().dereference())
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, ty: Type): Alloc {
            require(isAppropriateType(ty)) {
                "should not be $ty"
            }

            return Alloc(name, ty)
        }

        fun isAppropriateType(ty: Type): Boolean {
            return ty !is VoidType && ty !is UndefinedType
        }
    }
}