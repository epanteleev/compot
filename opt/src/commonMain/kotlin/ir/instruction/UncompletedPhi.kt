package ir.instruction

import ir.types.*
import ir.value.Value
import common.arrayWith
import common.forEachWith
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class UncompletedPhi private constructor(id: Identity, owner: Block, private val ty: PrimitiveType, private var incoming: MutableList<Block>, incomingValue: Array<Value>):
    ValueInstruction(id, owner, incomingValue) {
    override fun dump(): String = buildString {
        append("%${name()} = $NAME $ty [")
        zipWithIndex { value, bb, idx ->
            append("$bb: $value")
            if (idx != incoming.size - 1) {
                append(", ")
            }
        }
        append(']')
    }

    override fun type(): PrimitiveType = ty

    fun incoming(): List<Block> = incoming

    fun zipWithIndex(closure: (Block, Value, Int) -> Unit) {
        incoming().forEachWith(operands) { bb, value, i ->
            closure(bb, value, i)
        }
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "uncompleted-phi"

        fun phi(type: PrimitiveType, incoming: Value, predecessors: MutableList<Block>): InstBuilder<UncompletedPhi> = {
            id: Identity, owner: Block -> makeUncompleted(id, owner, type, incoming, predecessors)
        }

        private fun makeUncompleted(id: Identity, owner: Block, type: PrimitiveType, incoming: Value, predecessors: MutableList<Block>): UncompletedPhi {
            val incomingType = incoming.type()
            require(incomingType is PtrType) {
                "should be pointer type in '$id', type=$type, but incoming=$incoming:$incomingType"
            }

            val values = arrayWith(predecessors.size) { incoming }
            return UncompletedPhi(id, owner, type, predecessors, values)
        }
    }
}