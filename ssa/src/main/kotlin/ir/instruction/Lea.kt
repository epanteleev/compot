package ir.instruction

import ir.types.*
import ir.instruction.utils.Visitor


class Lea private constructor(name: String, origin: Generate):
    ValueInstruction(name, Type.Ptr, arrayOf(origin)) {

    override fun type(): PointerType {
        return Type.Ptr
    }

    override fun dump(): String {
        return "%$identifier = $NAME $tp ${generate()}"
    }

    fun generate(): Generate {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0] as Generate
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "lea"

        fun make(name: String, gen: Generate): Lea {
            val originType = gen.type()
            require(isAppropriateType(originType)) {
                "inconsistent type '$name' generate=$gen:$originType"
            }

            return registerUser(Lea(name, gen), gen)
        }

        fun typeCheck(lea: Lea): Boolean {
            return isAppropriateType(lea.generate().type())
        }

        private fun isAppropriateType(originType: Type): Boolean {
            return originType is PrimitiveType
        }
    }
}