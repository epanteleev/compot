package ir.instruction.lir

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.Generate
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor
import ir.global.FunctionSymbol
import ir.global.GlobalConstant
import ir.global.GlobalValue
import ir.instruction.Identity
import ir.module.block.Block


class Lea private constructor(id: Identity, owner: Block, value: Value):
    ValueInstruction(id, owner, Type.Ptr, arrayOf(value)) {

    override fun type(): PointerType {
        return Type.Ptr
    }

    override fun dump(): String {
        return "%${name()} = $NAME $tp ${operand()}"
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
                "should be '${NAME}' or global constant, but '$value'"
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