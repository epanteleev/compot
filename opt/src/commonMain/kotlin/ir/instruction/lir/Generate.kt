package ir.instruction.lir

import ir.instruction.Identity
import ir.instruction.InstBuilder
import ir.instruction.ValueInstruction
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Generate private constructor(id: Identity, owner: Block, private val allocatedType: NonTrivialType):
    ValueInstruction(id, owner, arrayOf()) {
    override fun dump(): String {
        return "%${name()} = $NAME ${type()}"
    }

    override fun type(): NonTrivialType = allocatedType

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gen"

        fun gen(ty: NonTrivialType): InstBuilder<Generate> = { id: Identity, owner: Block ->
            make(id, owner, ty)
        }

        private fun make(id: Identity, owner: Block, ty: NonTrivialType): Generate {
            require(isAppropriateType(ty)) {
                "should not be $ty in '$id', but expected a primitive type"
            }

            return Generate(id, owner, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty is NonTrivialType
        }

        fun typeCheck(alloc: Generate): Boolean {
            return isAppropriateType(alloc.type())
        }
    }
}