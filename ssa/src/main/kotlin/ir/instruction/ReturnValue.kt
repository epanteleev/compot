package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.NonTrivialType
import ir.types.Type
import ir.types.AnyType
import ir.types.VoidType

class ReturnValue private constructor(value: Value): Return(arrayOf(value)) {
    override fun dump(): String {
        return "ret ${value().type()} ${value()}"
    }

    fun type(): NonTrivialType {
        return value().type()
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(value: Value): Return {
            val retType = value.type()
            require(isAppropriateType(retType)) {
                "cannot be $retType"
            }

            return registerUser(ReturnValue(value), value)
        }

        private fun isAppropriateType(retType: Type): Boolean {
            return retType !is VoidType && retType !is AnyType
        }

        fun typeCheck(retValue: ReturnValue): Boolean {
            return isAppropriateType(retValue.value().type())
        }
    }
}