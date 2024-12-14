package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Truncate private constructor(id: Identity, owner: Block, private val toType: IntegerType, value: Value):
    ValueInstruction(id, owner, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[OPERAND]
    }

    override fun type(): IntegerType = toType

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "trunc"
        const val OPERAND = 0

        fun trunc(toType: IntegerType, value: Value): InstBuilder<Truncate> = { id: Identity, owner: Block ->
            make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: IntegerType, value: Value): Truncate {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id' type=$toType, value=$value:$valueType"
            }

            return registerUser(Truncate(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: IntegerType, valueType: Type): Boolean {
            if (toType is SignedIntType && valueType is SignedIntType) {
                return toType.sizeOf() < valueType.sizeOf()
            }
            if (toType is UnsignedIntType && valueType is UnsignedIntType) {
                return toType.sizeOf() < valueType.sizeOf()
            }

            return false
        }

        fun typeCheck(trunc: Truncate): Boolean {
            return isAppropriateType(trunc.type(), trunc.value().type())
        }
    }
}