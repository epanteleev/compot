package ir.instruction

import ir.value.*
import ir.module.block.Block
import ir.types.Type


abstract class ValueInstruction(id: Identity,
                                owner: Block,
                                protected val tp: Type,
                                operands: Array<Value>):
    Instruction(id, owner, operands),
    LocalValue {
    override var usedIn: MutableList<Instruction> = arrayListOf()

    final override fun name(): String {
        return "${owner.index}x${id}"
    }

    final override fun toString(): String {
        return "%${name()}"
    }
}