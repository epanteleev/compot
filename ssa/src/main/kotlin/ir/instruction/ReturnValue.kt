package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.module.block.Block
import ir.types.Type
import ir.types.UndefinedType
import ir.types.VoidType

class ReturnValue private constructor(value: Value): Return(arrayOf(value)) {
    override fun dump(): String {
        return "ret ${value().type()} ${value()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(usages: List<Value>, newTargets: Array<Block>): Return {
        return make(usages[0])
    }

    override fun copy(newUsages: List<Value>): Return {
        return make(newUsages[0])
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
            return retType !is VoidType && retType !is UndefinedType
        }

        fun isCorrect(retValue: ReturnValue): Boolean {
            return isAppropriateType(retValue.value().type())
        }
    }
}