package ir.instruction

import ir.*
import ir.module.block.Block
import ir.types.NonTrivialType
import ir.value.LocalValue
import ir.value.Value


abstract class TerminateValueInstruction(id: Identity, owner: Block,
                                         protected val tp: NonTrivialType,
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

    override fun type(): NonTrivialType = tp

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TerminateValueInstruction

        return id == other.id && owner.index == other.owner.index
    }

    override fun hashCode(): Int {
        return id + owner.index
    }
}