package ir.instruction

import ir.*
import ir.module.block.Block
import ir.types.PrimitiveType

class Phi(name: String, ty: PrimitiveType, private val incoming: Array<Block>, incomingValue: Array<Value>):
    ValueInstruction(name, ty, incomingValue) {

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$identifier = phi $tp [")
        operands.zip(incoming).joinTo(builder) {
            "${it.first}: ${it.second}"
        }
        builder.append(']')
        return builder.toString()
    }

    fun incoming(): Array<Block> {
        return incoming
    }

    fun zip(): List<Pair<Block, Value>> {
        return incoming() zip usages()
    }

    override fun copy(newUsages: List<Value>): Instruction {
        return Phi(identifier, tp as PrimitiveType, incoming.clone(), newUsages.toTypedArray())
    }

    fun copy(newUsages: Array<Value>, incoming: Array<Block>): Phi {
        return Phi(identifier, tp as PrimitiveType, incoming.clone(), newUsages.clone())
    }

    fun update(newUsages: Array<Value>, newIncoming: Array<Block>): Phi {
        update(newUsages.toList())
        newIncoming.copyInto(incoming)
        return this
    }

    companion object {
        fun create(name: String, ty: PrimitiveType): Phi {
            return Phi(name, ty, arrayOf(), arrayOf())
        }
    }
}