package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Select private constructor(id: Identity, owner: Block, private val ty: IntegerType, cond: Value, onTrue: Value, onFalse: Value) :
    ValueInstruction(id, owner, arrayOf(cond, onTrue, onFalse)) {
    override fun type(): IntegerType = ty

    override fun dump(): String {
        return "%${name()} = $NAME ${Type.U1} ${condition()}, ${onTrue().type()} ${onTrue()}, ${onFalse().type()} ${onFalse()}"
    }

    fun condition(): Value {
        assertion(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[0]
    }

    fun onTrue(): Value {
        assertion(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[1]
    }

    fun onFalse(): Value {
        assertion(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[2]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "select"

        fun make(id: Identity, owner: Block, ty: IntegerType, cond: Value, onTrue: Value, onFalse: Value): Select {
            val onTrueType = onTrue.type()
            val onFalseType = onFalse.type()
            val condType = cond.type()
            require(isAppropriateType(ty, condType, onTrueType, onFalseType)) {
                "inconsistent types: type=$ty, condition=$cond:$condType, onTrue=$onTrue:$onTrueType, onFalse=$onFalse:$onFalseType"
            }

            return registerUser(Select(id, owner, ty, cond, onTrue, onFalse), cond, onTrue, onFalse)
        }

        private fun isAppropriateType(ty: IntegerType, condType: Type, onTrueType: Type, onFalseType: Type): Boolean {
            if (condType !is FlagType) {
                return false
            }

            return (ty == onFalseType) && (ty == onTrueType)
        }

        fun typeCheck(select: Select): Boolean {
            return isAppropriateType(select.type(),
                select.condition().type(),
                select.onTrue().type(),
                select.onFalse().type())
        }
    }
}