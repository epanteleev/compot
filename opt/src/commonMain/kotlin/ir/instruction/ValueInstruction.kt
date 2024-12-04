package ir.instruction

import ir.value.*
import ir.module.block.Block


abstract class ValueInstruction(id: Identity, owner: Block, operands: Array<Value>):
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