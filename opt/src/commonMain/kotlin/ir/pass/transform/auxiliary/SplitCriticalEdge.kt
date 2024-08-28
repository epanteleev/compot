package ir.pass.transform.auxiliary

import ir.module.Module
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block


internal class SplitCriticalEdge private constructor(private val cfg: FunctionData) {
    private val predecessorMap = hashMapOf<Block, Block>()

    fun pass() {
        val basicBlocks = cfg

        for (bbIdx in 0 until basicBlocks.size()) {
            val predecessors = basicBlocks[bbIdx].predecessors()
            for (index in predecessors.indices) {
                if (!basicBlocks[bbIdx].hasCriticalEdgeFrom(predecessors[index])) {
                    continue
                }

                insertBasicBlock(basicBlocks[bbIdx], predecessors[index])
            }
        }

        updatePhi(cfg)
    }

    private fun updatePhi(basicBlocks: FunctionData) {
        for (bb in basicBlocks) {
            bb.phis { phi ->
                var changed = false
                phi.updateControlFlow { oldBB, _ ->
                    val p = predecessorMap[oldBB]
                    if (p != null) {
                        changed = true
                        p
                    } else {
                        oldBB
                    }
                }

                if (!changed) {
                    return@phis
                }
            }
        }
    }

    private fun insertBasicBlock(bb: Block, p: Block) {
        val newBlock = cfg.blocks.createBlock().apply {
            branch(bb)
        }

        val inst = p.last()
        inst.updateTargets {
            if (it == bb) {
                newBlock
            } else {
                it
            }
        }

        predecessorMap[p] = newBlock
        Block.insertBlock(bb, newBlock, p)
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { fnData ->
                SplitCriticalEdge(fnData).pass()
            }

            return module
        }
    }
}

fun Block.hasCriticalEdgeFrom(predecessor: Block): Boolean {
    return predecessor.successors().size > 1 && predecessors().size > 1
}