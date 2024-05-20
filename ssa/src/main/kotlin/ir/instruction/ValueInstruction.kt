package ir.instruction

import ir.*
import ir.module.block.Block
import ir.types.NonTrivialType

typealias Identity = Int

abstract class ValueInstruction(id: Identity,
                                owner: Block,
                                protected val tp: NonTrivialType,
                                operands: Array<Value>):
    Instruction(id, owner, operands),
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
        if (javaClass != other?.javaClass) return false

        other as ValueInstruction

        return id == other.id && owner.index == other.owner.index
    }

    override fun hashCode(): Int {
        return id + owner.index
    }
}