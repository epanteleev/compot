package ir.instruction

import common.arrayWrapperOf
import ir.types.*
import ir.value.asValue
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class UncompletedPhi private constructor(id: Identity, owner: Block, private val ty: PrimitiveType, private val incoming: Array<Block>, incomingValue: Alloc):
    ValueInstruction(id, owner, arrayOf(incomingValue)) {
    override fun dump(): String = buildString {
        append("%${name()} = $NAME $ty [${value()}] [")
        for (i in incoming.indices) {
            append(incoming[i])
            if (i != incoming.size - 1) {
                append(", ")
            }
        }
        append(']')
    }

    override fun type(): PrimitiveType = ty

    fun incoming(): List<Block> = arrayWrapperOf(incoming)

    fun value(): Alloc = operand(0).asValue()

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "uncompleted-phi"

        fun phi(type: PrimitiveType, incoming: Alloc, predecessors: Array<Block>): InstBuilder<UncompletedPhi> = {
            id: Identity, owner: Block -> make(id, owner, type, incoming, predecessors)
        }

        private fun make(id: Identity, owner: Block, type: PrimitiveType, incoming: Alloc, predecessors: Array<Block>): UncompletedPhi {
            return registerUser(UncompletedPhi(id, owner, type, predecessors, incoming), incoming)
        }
    }
}