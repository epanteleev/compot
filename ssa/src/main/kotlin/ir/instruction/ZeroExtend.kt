package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class ZeroExtend private constructor(name: String, toType: IntegerType, value: Value):
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

    override fun type(): IntegerType {
        return tp as IntegerType
    }

    override fun copy(newUsages: List<Value>): ZeroExtend {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, type(), newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        const val NAME = "zext"

        fun make(name: String, toType: IntegerType, value: Value): ZeroExtend {
            val valueType = value.type()
            require(isAppropriateType(toType, valueType)) {
                "inconsistent types in $name: ty=$toType, value.type=$valueType"
            }

            return registerUser(ZeroExtend(name, toType, value), value)
        }

        private fun isAppropriateType(toType: IntegerType, valueType: Type): Boolean {
            if (valueType !is IntegerType) {
                return false
            }

            return toType.size() > valueType.size()
        }

        fun isCorrect(zext: ZeroExtend): Boolean {
            return isAppropriateType(zext.type(), zext.value().type())
        }
    }
}