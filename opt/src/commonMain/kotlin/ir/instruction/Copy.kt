package ir.instruction

import ir.types.Type
import ir.value.Value
import ir.types.asType
import common.assertion
import ir.module.block.Block
import ir.types.PrimitiveType
import ir.value.constant.UndefinedValue
import ir.instruction.utils.IRInstructionVisitor


class Copy private constructor(id: Identity, owner: Block, private val type: PrimitiveType, origin: Value):
    ValueInstruction(id, owner, arrayOf(origin)) {

    override fun type(): PrimitiveType = type

    override fun dump(): String {
        return "%${name()} = $NAME $type ${origin()}"
    }

    fun origin(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "copy"

        fun make(id: Identity, owner: Block, origin: Value): Copy {
            val originType = origin.type()
            require(isAppropriateType(originType, origin)) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(Copy(id, owner, originType.asType(), origin), origin)
        }

        fun typeCheck(copy: Copy): Boolean {
            return isAppropriateType(copy.type(), copy.origin())
        }

        private fun isAppropriateType(originType: Type, origin: Value): Boolean {
            if (origin is UndefinedValue) {
                // Copy instruction can copy UndefinedValue
                return true
            }
            
            return originType is PrimitiveType && origin.type() == originType
        }
    }
}