package ir.instruction.lir

import ir.types.*
import ir.global.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.Identity
import ir.instruction.Generate
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor


class Lea private constructor(id: Identity, owner: Block, value: Value):
    ValueInstruction(id, owner, arrayOf(value)) {

    override fun type(): PtrType = PtrType

    override fun dump(): String {
        return "%${name()} = $NAME ${type()} ${operand()}"
    }

    fun operand(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "lea"

        fun make(id: Identity, owner: Block, value: Value): Lea {
            val originType = value.type()
            require(isAppropriateType(originType)) {
                "inconsistent type '$id' generate=$value:$originType"
            }
            require(value is Generate || value is GlobalConstant || value is FunctionSymbol || value is GlobalValue) {
                "should be '$NAME' or global constant, but '$value'"
            }

            return registerUser(Lea(id, owner, value), value)
        }

        fun typeCheck(lea: Lea): Boolean {
            return isAppropriateType(lea.operand().type())
        }

        private fun isAppropriateType(originType: Type): Boolean {
            return true//originType is PrimitiveType
        }
    }
}