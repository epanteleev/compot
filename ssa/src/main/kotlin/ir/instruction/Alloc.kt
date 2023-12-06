package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.PointerType
import ir.types.Type
import ir.types.UndefinedType
import ir.types.VoidType

class Alloc private constructor(name: String, val allocatedType: Type):
    ValueInstruction(name, allocatedType.ptr(), arrayOf()) {
    override fun dump(): String {
        return "%$identifier = alloc $allocatedType"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun copy(newUsages: List<Value>): Alloc {
        return make(identifier, allocatedType)
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

        private fun isAppropriateType(ty: Type): Boolean {
            return ty !is VoidType && ty !is UndefinedType
        }
    }
}