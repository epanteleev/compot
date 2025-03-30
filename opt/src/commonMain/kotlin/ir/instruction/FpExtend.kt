package ir.instruction

import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class FpExtend private constructor(id: Identity, owner: Block, toType: FloatingPointType, value: Value):
    Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}"
    }

    override fun type(): FloatingPointType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "fpext"

        fun fpext(value: Value, toType: FloatingPointType): InstBuilder<FpExtend> = {
            id: Identity, owner: Block -> make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: FloatingPointType, value: Value): FpExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(FpExtend(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: FloatingPointType, valueType: Type): Boolean {
            if (valueType !is FloatingPointType) {
                return false
            }

            return toType.sizeOf() > valueType.sizeOf()
        }

        fun typeCheck(fpext: FpExtend): Boolean {
            return isAppropriateType(fpext.type(), fpext.operand().type())
        }
    }
}