package ir.instruction

import common.assertion
import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class SignExtend private constructor(id: Identity, owner: Block, toType: SignedIntType, value: Value):
    ValueInstruction(id, owner, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): SignedIntType {
        return tp as SignedIntType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "sext"

        fun make(id: Identity, owner: Block, toType: SignedIntType, value: Value): SignExtend {
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

            return toType.size() > valueType.size()
        }

        fun typeCheck(sext: SignExtend): Boolean {
            return isAppropriateType(sext.type(), sext.value().type())
        }
    }
}