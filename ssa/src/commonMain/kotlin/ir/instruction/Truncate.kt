package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Truncate private constructor(id: Identity, owner: Block, toType: IntegerType, value: Value):
    ValueInstruction(id, owner, toType, arrayOf(value)) {
    private fun checkedGet(index: Int): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[index]
    }

    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value = checkedGet(0)

    override fun type(): IntegerType {
        return tp as IntegerType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "trunc"

        fun make(id: Identity, owner: Block, toType: IntegerType, value: Value): Truncate {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id' type=$toType, value=$value:$valueType"
            }

            return registerUser(Truncate(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: IntegerType, valueType: Type): Boolean {
            val isSameSign = (toType is SignedIntType && valueType is SignedIntType) ||
                    (toType is UnsignedIntType && valueType is UnsignedIntType)
            if (!isSameSign) {
                return false
            }

            valueType as IntegerType
            return toType.sizeOf() < valueType.sizeOf()
        }

        fun typeCheck(trunc: Truncate): Boolean {
            return isAppropriateType(trunc.type(), trunc.value().type())
        }
    }
}