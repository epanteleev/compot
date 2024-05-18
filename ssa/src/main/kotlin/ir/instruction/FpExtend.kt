package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class FpExtend private constructor(name: String, owner: Block, toType: FloatingPointType, value: Value):
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
        const val NAME = "fpext"

        fun make(name: String, owner: Block, toType: FloatingPointType, value: Value): FpExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value=$value:$valueType"
            }

            return registerUser(FpExtend(name, owner, toType, value), value)
        }

        private fun isAppropriateType(toType: FloatingPointType, valueType: Type): Boolean {
            if (valueType !is FloatingPointType) {
                return false
            }

            return toType.size() > valueType.size()
        }

        fun typeCheck(fpext: FpExtend): Boolean {
            return isAppropriateType(fpext.type(), fpext.value().type())
        }
    }
}