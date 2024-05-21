package ir.instruction

import ir.Value
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.ArithmeticType
import ir.types.TupleType
import ir.types.Type


class TupleArithmeticBinary private constructor(id: Identity, owner: Block, tp: TupleType, a: Value, val op: ArithmeticBinaryOp, b: Value) :
    TupleInstruction(id, owner, tp, arrayOf(a, b)) {
    override fun dump(): String {
        return "%${name()} = $op $tp ${first()}, ${second()}"
    }

    override fun type(): TupleType {
        return tp as TupleType
    }

    fun first(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun second(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(id: Identity, owner: Block, type: ArithmeticType, a: Value, op: ArithmeticBinaryOp, b: Value): TupleArithmeticBinary {
            val aType = a.type()
            val bType = b.type()
            val tp = TupleType(arrayOf(type, Type.U1))
            require(isAppropriateTypes(tp, aType, bType)) {
                "incorrect types in '$id' but type=$type, a=$a:$aType, b=$b:$bType"
            }

            return registerUser(TupleArithmeticBinary(id, owner, tp, a, op, b), a, b)
        }

        private fun isAppropriateTypes(tp: TupleType, aType: Type, bType: Type): Boolean {
            return aType == tp && bType == tp
        }

        fun typeCheck(binary: TupleArithmeticBinary): Boolean {
            return isAppropriateTypes(binary.type(), binary.first().type(), binary.second().type())
        }
    }
}