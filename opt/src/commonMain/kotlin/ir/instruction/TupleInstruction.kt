package ir.instruction

import ir.module.block.Block
import ir.types.TrivialType
import ir.types.TupleType
import ir.value.LocalValue
import ir.value.Value


abstract class TupleInstruction(id: Identity,
                       owner: Block,
                       protected val tp: TrivialType,
                       operands: Array<Value>):
    Instruction(id, owner, operands), LocalValue {
    override var usedIn: MutableList<Instruction> = mutableListOf()

    override fun name(): String {
        return "${owner.index}x${id}"
    }

    override fun toString(): String {
        return "%${name()}"
    }

    fun proj(index: Int): Projection? {
        for (user in usedIn()) {
            user as Projection
            if (user.index() == index) {
                return user
            }
        }
        return null
    }

    abstract override fun type(): TupleType
}