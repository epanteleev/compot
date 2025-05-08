package ir.instruction

import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Truncate private constructor(id: Identity, owner: Block, toType: IntegerType, value: Value): Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}" //TODO code duplication!!!
    }

    override fun type(): IntegerType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "trunc"

        fun trunc(value: Value, toType: IntegerType): InstBuilder<Truncate> = { id: Identity, owner: Block ->
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
            return isAppropriateType(trunc.type(), trunc.operand().type())
        }
    }
}