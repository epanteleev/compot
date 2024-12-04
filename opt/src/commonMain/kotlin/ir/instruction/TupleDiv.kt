package ir.instruction

import ir.types.*
import ir.value.*
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class TupleDiv private constructor(id: Identity, owner: Block, private val tp: TupleType, a: Value, b: Value) :
    ValueInstruction(id, owner, arrayOf(a, b)), TupleValue {
    override fun dump(): String {
        return "%${name()} = $NAME $tp, ${first().type()} ${first()}, ${second().type()} ${second()}"
    }

    override fun type(): TupleType = tp

    fun first(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun second(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun quotient(): Projection? {
        return proj(0)
    }

    fun remainder(): Projection? {
        return proj(1)
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "div"
        const val FIRST = 0
        const val SECOND = 1

        fun make(id: Identity, owner: Block, type: ArithmeticType, a: Value, b: Value): TupleDiv {
            val aType = a.type()
            val bType = b.type()
            val tp = TupleType(arrayOf(type, type))
            require(isAppropriateTypes(tp, aType, bType)) {
                "incorrect types in '$id' but type=$type, a=$a:$aType, b=$b:$bType"
            }

            return registerUser(TupleDiv(id, owner, tp, a, b), a, b)
        }

        private fun isAppropriateTypes(tp: TupleType, aType: Type, bType: Type): Boolean {
            return aType == tp.innerType(0) &&
                    bType == tp.innerType(1) &&
                    tp.innerType(1) == tp.innerType(0)
        }

        fun typeCheck(binary: TupleDiv): Boolean {
            return isAppropriateTypes(binary.type(), binary.first().type(), binary.second().type())
        }
    }
}