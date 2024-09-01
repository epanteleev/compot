package ir.instruction

import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.ArithmeticType
import ir.types.IntegerType
import ir.types.Type
import ir.value.Value

class Shr private constructor(id: Identity, owner: Block, tp: ArithmeticType, a: Value, b: Value) : ArithmeticBinary(id, owner, tp, a, b) {
    override fun dump(): String = "%${name()} = $NAME $tp ${first()}, ${second()}"

    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "shr"
        const val FIRST = 0
        const val SECOND = 1

        fun make(id: Identity, owner: Block, a: Value, b: Value): Shr {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateTypes(aType, aType, bType)) {
                "incorrect types in '$id' but type=$aType, a=$a:$aType, b=$b:$bType"
            }

            return registerUser(Shr(id, owner, aType as IntegerType, a, b), a, b)
        }

        private fun isAppropriateTypes(tp: Type, aType: Type, bType: Type): Boolean {
            if (tp !is IntegerType) {
                return false
            }
            return aType == tp && bType == tp
        }

        fun typeCheck(binary: ArithmeticBinary): Boolean {
            return isAppropriateTypes(binary.type(), binary.first().type(), binary.second().type())
        }
    }
}