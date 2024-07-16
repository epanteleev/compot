package ir.instruction

import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Generate private constructor(id: Identity, owner: Block, allocatedType: NonTrivialType):
    ValueInstruction(id, owner, allocatedType, arrayOf()) {
    override fun dump(): String {
        return "%${name()} = $NAME ${type()}"
    }

    override fun type(): NonTrivialType {
        return tp as NonTrivialType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gen"

        fun make(id: Identity, owner: Block, ty: NonTrivialType): Generate {
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