package ir.instruction

import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class ZeroExtend private constructor(id: Identity, owner: Block, toType: UnsignedIntType, value: Value):
    Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}"
    }

    override fun type(): UnsignedIntType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "zext"

        fun zext(value: Value, toType: UnsignedIntType): InstBuilder<ZeroExtend> = { id: Identity, owner: Block ->
            make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: UnsignedIntType, value: Value): ZeroExtend {
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
            return isAppropriateType(zext.type(), zext.operand().type())
        }
    }
}