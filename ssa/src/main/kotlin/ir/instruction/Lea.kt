package ir.instruction

import ir.Value
import ir.global.GlobalConstant
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class Lea private constructor(name: String, value: Value):
    ValueInstruction(name, Type.Ptr, arrayOf(value)) {

    override fun type(): PointerType {
        return Type.Ptr
    }

    override fun dump(): String {
        return "%$identifier = $NAME $tp ${operand()}"
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "lea"

        fun make(name: String, value: Value): Lea {
            val originType = value.type()
            require(isAppropriateType(originType)) {
                "inconsistent type '$name' generate=$value:$originType"
            }
            require(value is Generate || value is GlobalConstant) {
                "should be '${Generate.NAME}' or global constant, but '$value'"
            }

            return registerUser(Lea(name, value), value)
        }

        fun typeCheck(lea: Lea): Boolean {
            return isAppropriateType(lea.operand().type())
        }

        private fun isAppropriateType(originType: Type): Boolean {
            return originType is PrimitiveType
        }
    }
}