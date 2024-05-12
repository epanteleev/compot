package ir.instruction

import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class Generate private constructor(name: String, allocatedType: NonTrivialType):
    ValueInstruction(name, allocatedType, arrayOf()) {
    override fun dump(): String {
        return "%$identifier = $NAME ${type()}"
    }

    override fun type(): NonTrivialType {
        return tp
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gen"

        fun make(name: String, ty: NonTrivialType): Generate {
            require(isAppropriateType(ty)) {
                "should not be $ty, but expected a primitive type"
            }
            return Generate(name, ty)
        }

        private fun isAppropriateType(ty: Type): Boolean {
            return ty is NonTrivialType
        }

        fun typeCheck(alloc: Generate): Boolean {
            return isAppropriateType(alloc.type())
        }
    }
}