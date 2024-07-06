package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Bitcast private constructor(id: Identity, owner: Block, toType: NonTrivialType, value: Value):
    ValueInstruction(id, owner, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "bitcast"

        fun make(id: Identity, owner: Block, toType: PrimitiveType, value: Value): Bitcast {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(Bitcast(id, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: Type, valueType: Type): Boolean {
            return toType is NonTrivialType &&
                    valueType is NonTrivialType &&
                    valueType.sizeOf() == toType.sizeOf() &&
                    toType !is FloatingPointType
        }

        fun typeCheck(bitcast: Bitcast): Boolean {
            return isAppropriateType(bitcast.type(), bitcast.value().type())
        }
    }
}