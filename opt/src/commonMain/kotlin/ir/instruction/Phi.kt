package ir.instruction

import ir.types.*
import ir.value.Value
import common.arrayWith
import common.forEachWith
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Phi private constructor(id: Identity, owner: Block, private val ty: PrimitiveType, private var incoming: MutableList<Block>, incomingValue: Array<Value>):
    ValueInstruction(id, owner, incomingValue) {
    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%${name()} = phi $ty [")
        zipWithIndex { value, bb, idx ->
            builder.append("$bb: $value")
            if (idx != incoming.size - 1) {
                builder.append(", ")
            }
        }
        builder.append(']')
        return builder.toString()
    }

    override fun type(): PrimitiveType = ty

    fun incoming(): List<Block> = incoming

    // DO NOT USE THIS METHOD DIRECTLY
    internal fun updateIncoming(block: Block, idx: Int) {
        incoming[idx] = block
    }

    fun zip(closure: (Block, Value) -> Unit) {
        incoming().forEachWith(operands) { bb, value ->
            closure(bb, value)
        }
    }

    fun zipWithIndex(closure: (Block, Value, Int) -> Unit) {
        incoming().forEachWith(operands) { bb, value, i ->
            closure(bb, value, i)
        }
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "phi"

        fun phi(incoming: MutableList<Block>, incomingValue: Array<Value>): InstBuilder<Phi> = {
            id: Identity, owner: Block -> make(id, owner, incoming, incomingValue)
        }

        fun phiUncompleted(type: PrimitiveType, incoming: Value, predecessors: MutableList<Block>): InstBuilder<Phi> = {
            id: Identity, owner: Block -> makeUncompleted(id, owner, type, incoming, predecessors)
        }

        private fun make(id: Identity, owner: Block, incoming: MutableList<Block>, incomingValue: Array<Value>): Phi {
            val ty = incomingValue[0].type() as PrimitiveType
            return registerUser(Phi(id, owner, ty, incoming, incomingValue), incomingValue.iterator())
        }

        private fun makeUncompleted(id: Identity, owner: Block, type: PrimitiveType, incoming: Value, predecessors: MutableList<Block>): Phi {
            val incomingType = incoming.type()
            require(incomingType is PtrType) {
                "should be pointer type in '$id', type=$type, but incoming=$incoming:$incomingType"
            }

            val values = arrayWith(predecessors.size) { incoming }
            return Phi(id, owner, type, predecessors, values)
        }

        private fun isAppropriateTypes(type: PrimitiveType, incomingValue: Array<Value>): Boolean {
            for (use in incomingValue) {
                if (type != use.type() && use.type() != UndefType) {
                    return false
                }
            }

            return true
        }

        fun typeCheck(phi: Phi): Boolean {
            return isAppropriateTypes(phi.type(), phi.operands)
        }
    }
}