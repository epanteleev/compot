package ir.instruction

import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Xor private constructor(id: Identity, owner: Block, tp: ArithmeticType, a: Value, b: Value) : ArithmeticBinary(id, owner, tp, a, b) {
    override fun dump(): String {
        return "%${name()} = $NAME $tp ${lhs()}, ${rhs()}"
    }

    override fun type(): ArithmeticType = tp

    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "xor"
        const val FIRST = 0
        const val SECOND = 1

        fun xor(a: Value, b: Value): InstBuilder<Xor> = { id: Identity, owner: Block ->
            make(id, owner, a, b)
        }

        private fun make(id: Identity, owner: Block, a: Value, b: Value): Xor {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateTypes(aType, aType, bType)) {
                "incorrect types in '$id' a=$a:$aType, b=$b:$bType"
            }

            return registerUser(Xor(id, owner, aType as ArithmeticType, a, b), a, b)
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