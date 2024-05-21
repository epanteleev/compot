package ir.instruction

import ir.Value
import ir.module.block.Block
import ir.types.TrivialType


abstract class TupleInstruction(id: Identity,
                       owner: Block,
                       protected val tp: TrivialType,
                       operands: Array<Value>):
    Instruction(id, owner, operands) {

    fun name(): String {
        return "${owner.index}x${id}"
    }

    override fun toString(): String {
        return "%${name()}"
    }

    abstract fun type(): TrivialType

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