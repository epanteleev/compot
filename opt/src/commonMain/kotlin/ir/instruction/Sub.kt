package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.ArithmeticType
import ir.types.Type
import ir.types.UndefType
import ir.value.Value


class Sub private constructor(id: Identity, owner: Block, tp: ArithmeticType, a: Value, b: Value) : ArithmeticBinary(id, owner, tp, a, b) {
    override fun dump(): String = "%${name()} = $NAME $tp ${lhs()}, ${rhs()}"

    override fun type(): ArithmeticType = tp

    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "sub"
        const val FIRST = 0
        const val SECOND = 1

        fun sub(a: Value, b: Value): InstBuilder<Sub> = { id: Identity, owner: Block ->
            make(id, owner, a, b)
        }

        private fun make(id: Identity, owner: Block, a: Value, b: Value): Sub {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateTypes(aType, aType, bType)) {
                "incorrect types in '$id' a=$a:$aType, b=$b:$bType"
            }

            return registerUser(Sub(id, owner, aType as ArithmeticType, a, b), a, b)
        }

        private fun isAppropriateTypes(tp: Type, aType: Type, bType: Type): Boolean {
            if (aType !is ArithmeticType) {
                return false
            }
            if (bType !is ArithmeticType) {
                return false
            }
            if (aType == UndefType || bType == UndefType) {
                return true
            }
            return aType == tp && bType == tp
        }

        fun typeCheck(binary: ArithmeticBinary): Boolean {
            return isAppropriateTypes(binary.type(), binary.lhs().type(), binary.rhs().type())
        }
    }
}