package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class SignExtend private constructor(id: Identity, owner: Block, toType: SignedIntType, value: Value):
    Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}"
    }

    override fun type(): SignedIntType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "sext"

        fun sext(value: Value, toType: SignedIntType): InstBuilder<SignExtend> = { id: Identity, owner: Block ->
            make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: SignedIntType, value: Value): SignExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id' type=$toType, value=$value:$valueType"
            }

            return registerUser(SignExtend(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: SignedIntType, valueType: Type): Boolean {
            if (valueType !is SignedIntType) {
                return false
            }

            return toType.sizeOf() > valueType.sizeOf()
        }

        fun typeCheck(sext: SignExtend): Boolean {
            return isAppropriateType(sext.type(), sext.operand().type())
        }
    }
}