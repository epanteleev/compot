package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class FloatToInt private constructor(id: Identity, owner: Block, private val toType: IntegerType, value: Value):
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

    override fun type(): IntegerType = toType

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "fp2int"

        fun make(id: Identity, owner: Block, toType: IntegerType, value: Value): FloatToInt {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(FloatToInt(id, owner, toType, value), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is FloatingPointType
        }

        fun typeCheck(fpext: FloatToInt): Boolean {
            return isAppropriateType(fpext.value().type())
        }
    }
}