package ir.instruction

import ir.value.Value
import ir.value.asType
import ir.module.block.Block
import ir.types.ArithmeticType
import ir.instruction.utils.IRInstructionVisitor
import ir.types.asType


class Neg private constructor(id: Identity, owner: Block, tp: ArithmeticType, value: Value):
    Unary(id, owner, tp, value) {
    override fun dump(): String {
        return "%${name()} = $NAME $tp ${operand()}"
    }

    override fun type(): ArithmeticType = tp.asType()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "neg"

        fun neg(value: Value): InstBuilder<Neg> = { id: Identity, owner: Block ->
            make(id, owner, value)
        }

        private fun make(id: Identity, owner: Block, value: Value): Neg {
            return registerUser(Neg(id, owner, value.asType(), value), value)
        }

        fun typeCheck(unary: Neg): Boolean {
            return true
        }
    }
}