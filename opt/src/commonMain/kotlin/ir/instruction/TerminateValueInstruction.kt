package ir.instruction

import ir.value.*
import ir.types.*
import ir.module.block.Block


sealed class TerminateValueInstruction(
    id: Identity, owner: Block,
    protected val tp: Type,
    usages: Array<Value>,
    targets: Array<Block>
) :
    TerminateInstruction(id, owner, usages, targets),
    LocalValue {
    override var usedIn: MutableList<Instruction> = arrayListOf()

    final override fun name(): String {
        return "${owner().index}x${id}"
    }

    final override fun toString(): String {
        return "%${name()}"
    }
}