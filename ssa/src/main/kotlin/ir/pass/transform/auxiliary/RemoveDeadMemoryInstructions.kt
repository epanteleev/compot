package ir.pass.transform.auxiliary

import ir.instruction.Alloc
import ir.instruction.Instruction
import ir.instruction.Load
import ir.instruction.Store
import ir.module.BasicBlocks
import ir.module.Module
import ir.module.block.Block
import ir.pass.transform.utils.Utils
import ir.pass.transform.utils.Utils.isLocalVariable

class RemoveDeadMemoryInstructions private constructor(private val cfg: BasicBlocks) {
    private fun removeMemoryInstructions(bb: Block) {
        fun filter(instruction: Instruction): Boolean {
            return when(instruction) {
                is Alloc -> instruction.isLocalVariable()
                is Store -> instruction.isLocalVariable()
                is Load  -> instruction.isLocalVariable()
                else -> false
            }
        }

        bb.removeIf { filter(it) }
    }

    fun pass() {
        for (bb in cfg) {
            removeMemoryInstructions(bb)
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { fnData ->
                val cfg = fnData.blocks
                RemoveDeadMemoryInstructions(cfg).pass()
            }

            return module
        }
    }
}