package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Bitcast private constructor(id: Identity, owner: Block, val toType: IntegerType, value: Value):
    ValueInstruction(id, owner, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to $toType"
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
        const val NAME = "bitcast"

        fun bitcast(value: Value, toType: IntegerType): InstBuilder<Bitcast> = {
            id: Identity, owner: Block -> make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: IntegerType, value: Value): Bitcast {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(Bitcast(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: Type, valueType: Type): Boolean {
            if (toType !is IntegerType) {
                return false
            }
            if (valueType !is IntegerType) {
                return false
            }
            return valueType.sizeOf() == toType.sizeOf()
        }

        fun typeCheck(bitcast: Bitcast): Boolean {
            return isAppropriateType(bitcast.type(), bitcast.value().type())
        }
    }
}