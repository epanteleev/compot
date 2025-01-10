package ir.instruction

import ir.types.*
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Alloc private constructor(id: Identity, owner: Block, val allocatedType: NonTrivialType):
    ValueInstruction(id, owner, arrayOf()) {
    override fun dump(): String {
        return "%${name()} = $NAME $allocatedType"
    }

    override fun type(): PtrType = PtrType

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "alloc"

        fun alloc(ty: NonTrivialType): InstBuilder<Alloc> = {
            id: Identity, owner: Block -> make(id, owner, ty)
        }

        private fun make(id: Identity, owner: Block, ty: NonTrivialType): Alloc {
            require(isAppropriateType(ty)) {
                "should not be $ty, but type=$ty"
            }

            return Alloc(id, owner, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty !is VoidType && ty !is UndefType && ty !is FlagType
        }

        fun typeCheck(alloc: Alloc): Boolean {
            return isAppropriateType(alloc.allocatedType)
        }
    }
}

