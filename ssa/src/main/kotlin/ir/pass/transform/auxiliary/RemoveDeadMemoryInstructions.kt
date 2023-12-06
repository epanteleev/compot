package ir.pass.transform.auxiliary

import ir.instruction.Instruction
import ir.module.BasicBlocks
import ir.module.Module
import ir.module.block.Block
import ir.pass.transform.utils.Utils

class RemoveDeadMemoryInstructions private constructor(private val cfg: BasicBlocks) {
    private fun removeMemoryInstructions(bb: Block) {
        fun filter(instruction: Instruction): Boolean {
            return when {
                Utils.isStackAllocOfLocalVariable(instruction) -> true
                Utils.isStoreOfLocalVariable(instruction) -> true
                Utils.isLoadOfLocalVariable(instruction)  -> true
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