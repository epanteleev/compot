package ir.instruction.lir

import ir.Value
import ir.types.Type
import ir.types.PrimitiveType
import ir.types.ArithmeticType
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor


class CopyByIndex private constructor(name: String, origin: Value, index: Value):
    ValueInstruction(name, origin.type(), arrayOf(origin, index)) {

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%$identifier = $NAME $tp ${origin()}, ${index().type()} ${index()}"
    }

    fun origin(): Value {
        assert(operands.size == 2) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "copy"

        fun make(name: String, origin: Value, index: Value): CopyByIndex {
            val originType = origin.type()
            require(isAppropriateType(originType, origin, index.type())) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(CopyByIndex(name, origin, index), origin)
        }

        fun typeCheck(copy: CopyByIndex): Boolean {
            return isAppropriateType(copy.type(), copy.origin(), copy.index().type())
        }

        private fun isAppropriateType(originType: Type, origin: Value, index: Type): Boolean {
            return originType is PrimitiveType && origin.type() == originType && index is ArithmeticType
        }
    }
}