package ir.instruction

import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class FpTruncate private constructor(id: Identity, owner: Block, toType: FloatingPointType, value: Value):
    Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}"
    }

    override fun type(): FloatingPointType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "fptrunc"

        fun fptrunc(value: Value, toType: FloatingPointType): InstBuilder<FpTruncate> = {
            id: Identity, owner: Block -> make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: FloatingPointType, value: Value): FpTruncate {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(FpTruncate(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: FloatingPointType, valueType: Type): Boolean {
            if (valueType !is FloatingPointType) {
                return false
            }

            return toType.sizeOf() < valueType.sizeOf()
        }

        fun typeCheck(trunc: FpTruncate): Boolean {
            return isAppropriateType(trunc.type(), trunc.operand().type())
        }
    }
}