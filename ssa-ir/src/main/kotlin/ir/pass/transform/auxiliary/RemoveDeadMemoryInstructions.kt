package ir.pass.transform.auxiliary

import ir.*
import ir.block.Block
import ir.instruction.Instruction

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