package ir.instruction

import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Alloc private constructor(name: String, owner: Block, val allocatedType: NonTrivialType):
    ValueInstruction(name, owner, Type.Ptr, arrayOf()) {
    override fun dump(): String {
        return "%$id = $NAME $allocatedType"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "alloc"

        fun make(name: String, owner: Block, ty: NonTrivialType): Alloc {
            require(isAppropriateType(ty)) {
                "should not be $ty, but type=$ty"
            }

            return Alloc(name, owner, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty !is VoidType && ty !is AnyType
        }

        fun typeCheck(alloc: Alloc): Boolean {
            return isAppropriateType(alloc.allocatedType)
        }
    }
}