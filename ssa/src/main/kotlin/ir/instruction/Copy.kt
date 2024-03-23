package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.PrimitiveType
import ir.instruction.utils.Visitor


class Copy private constructor(name: String, origin: Value):
    ValueInstruction(name, origin.type(), arrayOf(origin)) {

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%$identifier = $NAME $tp ${origin()}"
    }

    fun origin(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "copy"

        fun make(name: String, origin: Value): Copy {
            val originType = origin.type()
            require(isAppropriateType(originType)) {
                "should not be $originType"
            }

            return registerUser(Copy(name, origin), origin)
        }

        fun isCorrect(copy: Copy): Boolean {
            return isAppropriateType(copy.origin().type())
        }

        private fun isAppropriateType(originType: Type): Boolean {
            return originType is PrimitiveType
        }
    }
}