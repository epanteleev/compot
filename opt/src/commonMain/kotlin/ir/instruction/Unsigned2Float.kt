package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.types.FloatingPointType
import ir.instruction.utils.IRInstructionVisitor


class Unsigned2Float private constructor(id: Identity, owner: Block, toType: FloatingPointType, value: Value):
    Unary(id, owner, toType, value) {
    override fun dump(): String {
        return "%${name()} = $NAME ${operand().type()} ${operand()} to ${type()}"
    }

    fun fromType(): UnsignedIntType {
        return operand().type().asType()
    }

    override fun type(): FloatingPointType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "uint2fp"

        fun uint2fp(value: Value, toType: FloatingPointType): InstBuilder<Unsigned2Float> = {
            id: Identity, owner: Block -> make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: FloatingPointType, value: Value): Unsigned2Float {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(Unsigned2Float(id, owner, toType, value), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is UnsignedIntType
        }

        fun typeCheck(uint2fp: Unsigned2Float): Boolean {
            return isAppropriateType(uint2fp.operand().type())
        }
    }
}