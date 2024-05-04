package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class Int2Float private constructor(name: String, toType: FloatingPointType, value: Value):
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

    override fun type(): FloatingPointType = tp as FloatingPointType

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "itofp"

        fun make(name: String, toType: FloatingPointType, value: Value): Int2Float {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "inconsistent types in $name: ty=$toType, value=$value:$valueType"
            }

            return registerUser(Int2Float(name, toType, value), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is IntegerType
        }

        fun typeCheck(fpext: Int2Float): Boolean {
            return isAppropriateType(fpext.value().type())
        }
    }
}