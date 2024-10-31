package ir.instruction

import common.ArrayWrapper
import common.assertion
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor
import ir.intrinsic.IntrinsicProvider


class Intrinsic private constructor(id: Identity, owner: Block, private val inputs: Array<Value>, val implementor: IntrinsicProvider, cont: Block)
    : TerminateInstruction(id, owner, inputs, arrayOf(cont)) {
    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("intrinsic")
        builder.append(" @${implementor.name}(")
        inputs.forEachIndexed { index, value ->
            builder.append("$value: ${value.type()}")
            if (index != inputs.size - 1) {
                builder.append(", ")
            }
        }
        builder.append(")")
        return builder.toString()
    }

    fun inputs(): ArrayWrapper<Value> {
        return ArrayWrapper(inputs)
    }

    fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 in $this instruction"
        }
        return targets[0]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(id: Identity, owner: Block, inputs: Array<Value>, implementor: IntrinsicProvider, cont: Block): Intrinsic {
            return registerUser(Intrinsic(id, owner, inputs, implementor, cont), *inputs)
        }

        fun typeCheck(intrinsic: Intrinsic): Boolean {
            return true
        }
    }
}