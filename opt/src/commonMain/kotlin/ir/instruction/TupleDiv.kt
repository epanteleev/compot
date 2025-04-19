package ir.instruction

import ir.types.*
import ir.value.*
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class TupleDiv private constructor(id: Identity, owner: Block, private val tp: TupleType, a: Value, b: Value) :
    ValueInstruction(id, owner, arrayOf(a, b)), TupleValue {
    override fun dump(): String {
        return "%${name()} = $NAME $tp, ${lhs().type()} ${lhs()}, ${rhs().type()} ${rhs()}"
    }

    override fun type(): TupleType = tp

    fun lhs(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun lhs(newValue: Value) {
        update(FIRST, newValue)
    }

    fun rhs(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun rhs(newValue: Value) {
        update(SECOND, newValue)
    }

    fun quotient(): Projection {
        return proj(0) ?: throw IllegalStateException("quotient is not found in $this")
    }

    fun remainder(): Projection {
        return proj(1) ?: throw IllegalStateException("remainder is not found in $this")
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "div"
        private const val FIRST = 0
        private const val SECOND = 1

        fun div(a: Value, b: Value): InstBuilder<TupleDiv> = {
            id: Identity, owner: Block -> make(id, owner, a, b)
        }

        private fun make(id: Identity, owner: Block, a: Value, b: Value): TupleDiv {
            val aType = a.type().asType<ArithmeticType>()
            val bType = b.type()
            val tp = TupleType(arrayOf(aType, aType))
            require(isAppropriateTypes(tp, aType, bType)) {
                "incorrect types in '$id' but a=$a:$aType, b=$b:$bType"
            }

            return registerUser(TupleDiv(id, owner, tp, a, b), a, b)
        }

        private fun isAppropriateTypes(tp: TupleType, aType: Type, bType: Type): Boolean {
            return aType == tp.innerType(0) &&
                    bType == tp.innerType(1) &&
                    tp.innerType(1) == tp.innerType(0)
        }

        fun typeCheck(binary: TupleDiv): Boolean {
            return isAppropriateTypes(binary.type(), binary.lhs().type(), binary.rhs().type())
        }
    }
}