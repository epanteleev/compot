package ir.instruction

import ir.value.Value
import ir.module.block.Block

abstract class Return(id: Identity, owner: Block, usages: Array<Value>):
    TerminateInstruction(id, owner, usages, arrayOf()) {

    companion object {
        const val NAME = "ret"
    }
}