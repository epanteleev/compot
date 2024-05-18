package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class FpTruncate private constructor(name: String, owner: Block, toType: FloatingPointType, value: Value):
    ValueInstruction(name, owner, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%$id = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): FloatingPointType {
        return tp as FloatingPointType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "fptrunc"

        fun make(name: String, owner: Block, toType: FloatingPointType, value: Value): FpTruncate {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value=$value:$valueType"
            }

            return registerUser(FpTruncate(name, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: FloatingPointType, valueType: Type): Boolean {
            if (valueType !is FloatingPointType) {
                return false
            }

            return toType.size() < valueType.size()
        }

        fun typeCheck(trunc: FpTruncate): Boolean {
            return isAppropriateType(trunc.type(), trunc.value().type())
        }
    }
}