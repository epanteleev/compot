package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class Bitcast private constructor(name: String, toType: NonTrivialType, value: Value):
    ValueInstruction(name, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%$identifier = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
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

        fun make(name: String, toType: PrimitiveType, value: Value): Bitcast {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value=$value:$valueType"
            }

            return registerUser(Bitcast(name, toType, value), value)
        }

        private fun isAppropriateType(toType: NonTrivialType, valueType: NonTrivialType): Boolean {
            return valueType.size() == toType.size() && toType !is FloatingPointType
        }

        fun typeCheck(bitcast: Bitcast): Boolean {
            return isAppropriateType(bitcast.type(), bitcast.value().type())
        }
    }
}