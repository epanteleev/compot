package ir.instruction

import ir.types.*
import ir.instruction.utils.Visitor


class Alloc private constructor(name: String, val allocatedType: Type):
    ValueInstruction(name, Type.Ptr, arrayOf()) {
    override fun dump(): String {
        return "%$identifier = $NAME $allocatedType"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "alloc"

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