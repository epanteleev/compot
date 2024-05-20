package ir.instruction

import ir.*
import ir.module.block.Block
import ir.types.NonTrivialType


abstract class TerminateValueInstruction(id: Identity, owner: Block,
                                         protected val tp: NonTrivialType,
                                         usages: Array<Value>,
                                         targets: Array<Block>):
    TerminateInstruction(id, owner, usages, targets), LocalValue {
    private var usedIn: MutableList<Instruction> = arrayListOf()

    override fun addUser(instruction: Instruction) {
        usedIn.add(instruction)
    }

    override fun killUser(instruction: Instruction) {
        usedIn.remove(instruction)
    }

    override fun release(): List<Instruction> {
        val result = usedIn
        usedIn = arrayListOf()
        return result
    }

    override fun usedIn(): List<Instruction> {
        return usedIn
    }

    override fun name(): String {
        return "${owner.index}x${id}"
    }

    override fun toString(): String {
        return "%${name()}"
    }

    override fun type(): NonTrivialType = tp

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TerminateValueInstruction

        return id == other.id && owner.index == other.owner.index
    }

    override fun hashCode(): Int {
        return id + owner.index
    }
}