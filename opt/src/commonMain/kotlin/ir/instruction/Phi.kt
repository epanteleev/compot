package ir.instruction

import common.arrayWith
import ir.value.Value
import ir.types.*
import ir.module.block.Block
import common.forEachWith
import ir.instruction.utils.IRInstructionVisitor


class Phi private constructor(id: Identity, owner: Block, ty: PrimitiveType, private var incoming: MutableList<Block>, incomingValue: Array<Value>):
    ValueInstruction(id, owner, ty, incomingValue) {
    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%${name()} = phi $tp [")
        zipWithIndex { value, bb, idx ->
            builder.append("$bb: $value")
            if (idx != incoming.size - 1) {
                builder.append(", ")
            }
        }
        builder.append(']')
        return builder.toString()
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    fun incoming(): List<Block> = incoming

    fun zip(closure: (Block, Value) -> Unit) {
        incoming().forEachWith(operands()) { bb, value ->
            closure(bb, value)
        }
    }

    fun zipWithIndex(closure: (Block, Value, Int) -> Unit) {
        incoming().forEachWith(operands()) { bb, value, i ->
            closure(bb, value, i)
        }
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    fun updateDataFlow(closure: (Block, Value) -> Value) {
        zipWithIndex { bb, value, idx ->
            update(idx, closure(bb, value))
        }
    }

    fun updateControlFlow(closure: (Block, Value) -> Block) {
        zipWithIndex { bb, value, idx ->
            incoming[idx] = closure(bb, value)
        }
    }

    companion object {
        fun make(id: Identity, owner: Block, ty: PrimitiveType): Phi {
            return Phi(id, owner, ty, arrayListOf(), arrayOf())
        }

        fun make(id: Identity, owner: Block, ty: PrimitiveType, incoming: MutableList<Block>, incomingValue: Array<Value>): Phi {
            return registerUser(Phi(id, owner, ty, incoming, incomingValue), incomingValue.iterator())
        }

        fun makeUncompleted(id: Identity, owner: Block, type: PrimitiveType, incoming: Value, predecessors: MutableList<Block>): Phi {
            val incomingType = incoming.type()
            require(incomingType is PointerType) {
                "should be pointer type in '$id', type=$type, but incoming=$incoming:$incomingType"
            }

            val values = arrayWith(predecessors.size) { incoming }
            return Phi(id, owner, type, predecessors, values)
        }

        private fun isAppropriateTypes(type: PrimitiveType, incomingValue: Array<Value>): Boolean {
            for (use in incomingValue) {
                if (type != use.type() && use.type() != Type.UNDEF) {
                    return false
                }
            }

            return true
        }

        fun typeCheck(phi: Phi): Boolean {
            return isAppropriateTypes(phi.type(), phi.operands())
        }
    }
}