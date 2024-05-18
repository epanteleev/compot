package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.PrimitiveType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Copy private constructor(name: String, owner: Block, origin: Value):
    ValueInstruction(name, owner, origin.type(), arrayOf(origin)) {

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%$id = $NAME $tp ${origin()}"
    }

    fun origin(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "copy"

        fun make(name: String, owner: Block, origin: Value): Copy {
            val originType = origin.type()
            require(isAppropriateType(originType, origin)) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(Copy(name, owner, origin), origin)
        }

        fun typeCheck(copy: Copy): Boolean {
            return isAppropriateType(copy.type(), copy.origin())
        }

        private fun isAppropriateType(originType: Type, origin: Value): Boolean {
            return originType is PrimitiveType && origin.type() == originType
        }
    }
}