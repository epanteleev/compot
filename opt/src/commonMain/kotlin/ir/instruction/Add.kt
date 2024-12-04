package ir.instruction

import ir.types.Type
import ir.value.Value
import ir.module.block.Block
import ir.types.ArithmeticType
import ir.instruction.utils.IRInstructionVisitor


class Add private constructor(id: Identity, owner: Block, tp: ArithmeticType, a: Value, b: Value) : ArithmeticBinary(id, owner, tp, a, b) {
    override fun dump(): String = "%${name()} = $NAME $tp ${lhs()}, ${rhs()}"

    override fun type(): ArithmeticType = tp

    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "add"
        const val FIRST = 0
        const val SECOND = 1

        fun make(id: Identity, owner: Block, a: Value, b: Value): Add {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateTypes(aType, aType, bType)) {
                "incorrect types in '$id' a=$a:$aType, b=$b:$bType"
            }

            return registerUser(Add(id, owner, aType as ArithmeticType, a, b), a, b)
        }

        private fun isAppropriateTypes(tp: Type, aType: Type, bType: Type): Boolean {
            if (tp !is ArithmeticType) {
                return false
            }
            return aType == tp && bType == tp
        }

        fun typeCheck(binary: ArithmeticBinary): Boolean {
            return isAppropriateTypes(binary.type(), binary.lhs().type(), binary.rhs().type())
        }
    }
}