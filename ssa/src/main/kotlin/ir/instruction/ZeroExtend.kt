package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class ZeroExtend private constructor(name: String, owner: Block, toType: UnsignedIntType, value: Value):
    ValueInstruction(name, owner, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%$id = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): UnsignedIntType {
        return tp as UnsignedIntType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "zext"

        fun make(name: String, owner: Block, toType: UnsignedIntType, value: Value): ZeroExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: type=$toType, value=$value:$valueType"
            }

            return registerUser(ZeroExtend(name, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: UnsignedIntType, valueType: Type): Boolean {
            if (valueType !is UnsignedIntType) {
                return false
            }

            return toType.size() > valueType.size()
        }

        fun typeCheck(zext: ZeroExtend): Boolean {
            return isAppropriateType(zext.type(), zext.value().type())
        }
    }
}