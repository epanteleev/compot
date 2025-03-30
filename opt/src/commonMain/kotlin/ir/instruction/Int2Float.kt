package ir.instruction

import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Int2Float private constructor(id: Identity, owner: Block, toType: FloatingPointType, value: Value):
    Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}"
    }

    fun fromType(): SignedIntType {
        return operand().type().asType()
    }

    override fun type(): FloatingPointType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "int2fp"

        fun int2fp(value: Value, toType: FloatingPointType): InstBuilder<Int2Float> = { id: Identity, owner: Block ->
            make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: FloatingPointType, value: Value): Int2Float {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(Int2Float(id, owner, toType, value), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is SignedIntType
        }

        fun typeCheck(fpext: Int2Float): Boolean {
            return isAppropriateType(fpext.operand().type())
        }
    }
}