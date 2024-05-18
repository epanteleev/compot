package ir.instruction

import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Generate private constructor(name: String, owner: Block, allocatedType: NonTrivialType):
    ValueInstruction(name, owner, allocatedType, arrayOf()) {
    override fun dump(): String {
        return "%$id = $NAME ${type()}"
    }

    override fun type(): NonTrivialType {
        return tp
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gen"

        fun make(name: String, owner: Block, ty: NonTrivialType): Generate {
            require(isAppropriateType(ty)) {
                "should not be $ty, but expected a primitive type"
            }
            return Generate(name, owner, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty is NonTrivialType
        }

        fun typeCheck(alloc: Generate): Boolean {
            return isAppropriateType(alloc.type())
        }
    }
}