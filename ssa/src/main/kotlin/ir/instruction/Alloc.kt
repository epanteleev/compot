package ir.instruction

import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class Alloc private constructor(name: String, val allocatedType: NonTrivialType):
    ValueInstruction(name, Type.Ptr, arrayOf()) {
    override fun dump(): String {
        return "%$identifier = $NAME $allocatedType"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "alloc"

        fun make(name: String, ty: NonTrivialType): Alloc {
            require(isAppropriateType(ty)) {
                "should not be $ty, but type=$ty"
            }

            return Alloc(name, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty !is VoidType && ty !is AnyType
        }

        fun typeCheck(alloc: Alloc): Boolean {
            return isAppropriateType(alloc.allocatedType)
        }
    }
}