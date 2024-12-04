package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.types.FloatingPointType
import ir.instruction.utils.IRInstructionVisitor


class Unsigned2Float private constructor(id: Identity, owner: Block, private val toType: FloatingPointType, value: Value):
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

    fun fromType(): UnsignedIntType {
        return value().type().asType()
    }

    override fun type(): FloatingPointType = toType

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "uint2fp"
        const val VALUE = 0

        fun make(id: Identity, owner: Block, toType: FloatingPointType, value: Value): Unsigned2Float {
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
            return isAppropriateType(uint2fp.value().type())
        }
    }
}