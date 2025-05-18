package ir.instruction

import ir.types.Type
import ir.value.Value
import ir.types.asType
import ir.module.block.Block
import ir.types.PrimitiveType
import ir.value.constant.UndefValue
import ir.instruction.utils.IRInstructionVisitor
import ir.value.asType


class Copy private constructor(id: Identity, owner: Block, private val type: PrimitiveType, origin: Value):
    Unary(id, owner, type, origin) {

    override fun type(): PrimitiveType = type

    override fun dump(): String {
        return "%${name()} = $NAME $type ${operand()}"
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "copy"

        fun copy(origin: Value): InstBuilder<Copy> = { id: Identity, owner: Block ->
            make(id, owner, origin.asType(), origin)
        }

        fun copy(ty: PrimitiveType, origin: Value): InstBuilder<Copy> = { id: Identity, owner: Block ->
            make(id, owner, ty, origin)
        }

        private fun make(id: Identity, owner: Block, originType: PrimitiveType, origin: Value): Copy {
            require(isAppropriateType(originType, origin)) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(Copy(id, owner, originType.asType(), origin), origin)
        }

        fun typeCheck(copy: Copy): Boolean {
            return isAppropriateType(copy.type(), copy.operand())
        }

        private fun isAppropriateType(originType: Type, origin: Value): Boolean {
            if (origin is UndefValue) {
                // Copy instruction can copy UndefinedValue
                return true
            }
            
            return originType is PrimitiveType
        }
    }
}