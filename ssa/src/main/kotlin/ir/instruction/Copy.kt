package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.PrimitiveType
import ir.types.Type

class Copy private constructor(name: String, origin: Value):
    ValueInstruction(name, origin.type(), arrayOf(origin)) {
    override fun dump(): String {
        return "%$identifier = copy $tp ${origin()}"
    }

    fun origin(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(newUsages: List<Value>): Copy {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
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