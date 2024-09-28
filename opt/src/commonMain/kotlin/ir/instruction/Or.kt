package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.types.Type
import ir.value.Value
import ir.module.block.Block
import ir.types.ArithmeticType
import ir.types.IntegerType


class Or private constructor(id: Identity, owner: Block, tp: IntegerType, a: Value, b: Value) : ArithmeticBinary(id, owner, tp, a, b) {
    override fun dump(): String = "%${name()} = $NAME $tp ${lhs()}, ${rhs()}"

    override fun type(): IntegerType = tp as IntegerType

    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "or"
        const val FIRST = 0
        const val SECOND = 1

        fun make(id: Identity, owner: Block, a: Value, b: Value): Or {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateTypes(aType, aType, bType)) {
                "incorrect types in '$id' a=$a:$aType, b=$b:$bType"
            }

            return registerUser(Or(id, owner, aType as IntegerType, a, b), a, b)
        }

        private fun isAppropriateTypes(tp: Type, aType: Type, bType: Type): Boolean {
            if (tp !is IntegerType) {
                return false
            }
            return aType == tp && bType == tp
        }

        fun typeCheck(binary: ArithmeticBinary): Boolean {
            return isAppropriateTypes(binary.type(), binary.lhs().type(), binary.rhs().type())
        }
    }
}