package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.module.block.Block
import common.forEachWith
import ir.instruction.utils.IRInstructionVisitor


class Phi private constructor(id: Identity, owner: Block, ty: PrimitiveType, private var incoming: List<Block>, incomingValue: Array<Value>):
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
        incoming().forEachWith(operands().asIterable()) { bb, value ->
            closure(bb, value)
        }
    }

    fun zipWithIndex(closure: (Block, Value, Int) -> Unit) {
        incoming().forEachWith(operands().asIterable()) { bb, value, i ->
            closure(bb, value, i)
        }
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    fun update(newUsages: List<Value>, newIncoming: List<Block>): Phi {
        assertion(newUsages.size == newIncoming.size) {
            "inconsistent size: usages=${newUsages.size}, incoming=${newIncoming.size}"
        }
        update(newUsages)
        incoming = newIncoming
        return this
    }

    companion object {
        fun make(id: Identity, owner: Block, ty: PrimitiveType): Phi {
            return Phi(id, owner, ty, arrayListOf(), arrayOf())
        }

        fun make(id: Identity, owner: Block, ty: PrimitiveType, incoming: List<Block>, incomingValue: Array<Value>): Phi {
            return registerUser(Phi(id, owner, ty, incoming, incomingValue), incomingValue.iterator())
        }

        fun makeUncompleted(id: Identity, owner: Block, type: PrimitiveType, incoming: Value, predecessors: List<Block>): Phi {
            val incomingType = incoming.type()
            require(incomingType is PointerType) {
                "should be pointer type in '$id', type=$type, but incoming=$incoming:$incomingType"
            }

            val values = predecessors.mapTo(arrayListOf()) { incoming }.toTypedArray() //Todo
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