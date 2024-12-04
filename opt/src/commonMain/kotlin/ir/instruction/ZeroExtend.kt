package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class ZeroExtend private constructor(id: Identity, owner: Block, private val toType: UnsignedIntType, value: Value):
    ValueInstruction(id, owner, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): UnsignedIntType = toType

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "zext"

        fun make(id: Identity, owner: Block, toType: UnsignedIntType, value: Value): ZeroExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id': type=$toType, value=$value:$valueType"
            }

            return registerUser(ZeroExtend(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: UnsignedIntType, valueType: Type): Boolean {
            if (valueType !is UnsignedIntType) {
                return false
            }

            return toType.sizeOf() > valueType.sizeOf()
        }

        fun typeCheck(zext: ZeroExtend): Boolean {
            return isAppropriateType(zext.type(), zext.value().type())
        }
    }
}