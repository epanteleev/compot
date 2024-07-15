package ir.instruction

import common.assertion
import ir.*
import ir.types.*
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor
import ir.value.Value


class Projection private constructor(id: Identity, owner: Block, type: NonTrivialType, tuple: TupleInstruction, private val index: Int):
    ValueInstruction(id, owner, type, arrayOf(tuple)) {

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%${name()} = $NAME $tp ${tuple()}, ${index()}"
    }

    fun tuple(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Int = index

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "proj"

        fun make(id: Identity, owner: Block, tuple: TupleInstruction, index: Int): Projection {
            val tupleType = tuple.type()
            val retType = tupleType.innerType(index)
            return registerUser(Projection(id, owner, retType, tuple, index), tuple) // TODO
        }

        fun typeCheck(proj: Projection): Boolean {
            return isAppropriateType(proj.type())
        }

        private fun isAppropriateType(projType: Type): Boolean {
            return projType is PrimitiveType
        }
    }
}