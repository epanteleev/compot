package ir.instruction

import common.arrayWrapperOf
import common.assertion
import ir.types.*
import ir.value.Value
import common.forEachWith
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Phi private constructor(id: Identity, owner: Block, private val ty: PrimitiveType, private var incoming: Array<Block>, incomingValue: Array<Value>):
    ValueInstruction(id, owner, incomingValue) {
    override fun dump(): String = buildString {
        append("%${name()} = phi $ty [")
        zipWithIndex { value, bb, idx ->
            append("$bb: $value")
            if (idx != incoming.size - 1) {
                append(", ")
            }
        }
        append(']')
    }

    override fun type(): PrimitiveType = ty

    fun values(): List<Block> = arrayWrapperOf(incoming)

    fun value(idx: Int, newValue: Value) {
        update(idx, newValue)
    }

    fun values(closure: (Block, Value) -> Value) {
        zipWithIndex { bb, value, idx ->
            update(idx, closure(bb, value))
        }
    }

    fun incoming(closure: (Block, Value) -> Block) = owner().cf {
        zipWithIndex { bb, value, idx ->
            updateIncoming(closure(bb, value), idx)
        }
    }

    private fun updateIncoming(block: Block, idx: Int) {
        incoming[idx] = block
    }

    fun zip(closure: (Block, Value) -> Unit) {
        values().forEachWith(operands) { bb, value ->
            closure(bb, value)
        }
    }

    fun zipWithIndex(closure: (Block, Value, Int) -> Unit) {
        values().forEachWith(operands) { bb, value, i ->
            closure(bb, value, i)
        }
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "phi"

        fun phi(incoming: Array<Block>, valueTypes: PrimitiveType, incomingValue: Array<Value>): InstBuilder<Phi> = {
            id: Identity, owner: Block -> make(id, owner, valueTypes, incoming, incomingValue)
        }

        private fun make(id: Identity, owner: Block, ty: PrimitiveType, incoming: Array<Block>, incomingValue: Array<Value>): Phi {
            return registerUser(Phi(id, owner, ty, incoming, incomingValue), incomingValue.iterator())
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