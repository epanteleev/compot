package ir.instruction

import ir.Value
import ir.module.block.Block

abstract class Return(id: Identity, owner: Block, usages: Array<Value>):
    TerminateInstruction(id, owner, usages, arrayOf())