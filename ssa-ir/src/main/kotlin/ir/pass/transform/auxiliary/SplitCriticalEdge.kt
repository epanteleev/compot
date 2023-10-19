package ir.pass.transform.auxiliary

import ir.module.BasicBlocks
import ir.module.block.Block
import ir.module.Module

internal class SplitCriticalEdge private constructor(private val cfg: BasicBlocks) {
    private var maxIndex = cfg.maxBlockIndex()
    private val predecessorMap = hashMapOf<Block, Block>()

    fun pass() {
        val basicBlocks = cfg.blocks()

        for (bbIdx in 0 until basicBlocks.size) {
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

    private fun updatePhi(basicBlocks: BasicBlocks) {
        for (bb in basicBlocks) {
            bb.phis { phi ->
                var changed = false
                val validIncoming = phi.incoming().map {
                    val p = predecessorMap[it]
                    if (p != null) {
                        changed = true
                        p
                    } else {
                        it
                    }
                }

                if (!changed) {
                    return@phis
                }

                phi.update(phi.usages(), validIncoming.toTypedArray())
            }
        }
    }

    private fun insertBasicBlock(bb: Block, p: Block) {
        maxIndex += 1
        val newBlock = Block.empty(maxIndex).apply {
            branch(bb)
        }

        val inst = p.last()
        val targets = inst.targets()
        val newTargets = targets.mapTo(arrayListOf()) {
            if (it == bb) {
                newBlock
            } else {
                it
            }
        }
        inst.updateTargets(newTargets)

        predecessorMap[p] = newBlock
        Block.insertBlock(bb, newBlock, p)

        cfg.putBlock(newBlock)
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { fnData ->
                val cfg = fnData.blocks
                SplitCriticalEdge(cfg).pass()
            }

            return module
        }
    }
}