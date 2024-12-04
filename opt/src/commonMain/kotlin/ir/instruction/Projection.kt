package ir.instruction

import ir.types.*
import ir.value.Value
import ir.value.asType
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Projection private constructor(id: Identity, owner: Block, private val type: PrimitiveType, tuple: Value, private val index: Int):
    ValueInstruction(id, owner, arrayOf(tuple)) {

    override fun type(): PrimitiveType = type

    override fun dump(): String {
        return "%${name()} = $NAME $type ${tuple()}, ${index()}"
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

        fun make(id: Identity, owner: Block, tuple: Value, index: Int): Projection {
            val tupleType = tuple.asType<TupleType>()
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