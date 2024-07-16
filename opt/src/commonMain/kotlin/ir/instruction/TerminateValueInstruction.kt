package ir.instruction

import ir.value.*
import ir.types.*
import ir.module.block.Block


abstract class TerminateValueInstruction(id: Identity, owner: Block,
                                         protected val tp: Type,
                                         usages: Array<Value>,
                                         targets: Array<Block>):
    TerminateInstruction(id, owner, usages, targets),
    LocalValue {
    override var usedIn: MutableList<Instruction> = arrayListOf()

    override fun name(): String {
        return "${owner.index}x${id}"
    }

    override fun toString(): String {
        return "%${name()}"
    }

    override fun type(): Type = tp
}