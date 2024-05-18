package ir.instruction

import ir.Value
import ir.module.block.Block

abstract class Return(owner: Block, usages: Array<Value>): TerminateInstruction(owner, usages, arrayOf())