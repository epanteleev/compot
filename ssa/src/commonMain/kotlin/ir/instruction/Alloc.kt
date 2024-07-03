package ir.instruction

import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Alloc private constructor(id: Identity, owner: Block, val allocatedType: NonTrivialType):
    ValueInstruction(id, owner, Type.Ptr, arrayOf()) {
    override fun dump(): String {
        return "%${name()} = $NAME $allocatedType"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "alloc"

        fun make(id: Identity, owner: Block, ty: NonTrivialType): Alloc {
            require(isAppropriateType(ty)) {
                "should not be $ty, but type=$ty"
            }

            return Alloc(id, owner, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty !is VoidType && ty !is AnyType
        }

        fun typeCheck(alloc: Alloc): Boolean {
            return isAppropriateType(alloc.allocatedType)
        }
    }
}