package ir.instruction

import common.assertion
import ir.value.Value
import ir.value.asType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.NonTrivialType
import ir.types.Type
import ir.types.BottomType
import ir.types.VoidType


class ReturnValue private constructor(id: Identity, owner: Block, value: Value): Return(id, owner, arrayOf(value)) {
    override fun dump(): String {
        return "ret ${value().type()} ${value()}"
    }

    fun type(): NonTrivialType {
        return value().asType()
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(id: Identity, owner: Block, value: Value): Return {
            val retType = value.type()
            require(isAppropriateType(retType)) {
                "cannot be $retType, but value=$value:${retType}"
            }

            return registerUser(ReturnValue(id, owner, value), value)
        }

        private fun isAppropriateType(retType: Type): Boolean {
            return retType !is VoidType && retType !is BottomType
        }

        fun typeCheck(retValue: ReturnValue): Boolean {
            return isAppropriateType(retValue.value().type())
        }
    }
}