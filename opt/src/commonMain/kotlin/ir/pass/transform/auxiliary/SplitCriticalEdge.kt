package ir.pass.transform.auxiliary

import ir.module.Module
import ir.module.FunctionData
import ir.module.block.Block


internal class SplitCriticalEdge private constructor(private val functionData: FunctionData) {
    private val predecessorMap = hashMapOf<Block, Block>()
    private val criticalEdgeBetween = hashMapOf<Block, Block>()

    fun pass() {
        for (fd in functionData) {
            for (p in fd.predecessors()) {
                if (!fd.hasCriticalEdgeFrom(p)) {
                    continue
                }

                criticalEdgeBetween[p] = fd
            }
        }

        for ((p, bb) in criticalEdgeBetween) {
            insertBasicBlock(bb, p)
        }

        updatePhi()
    }

    private fun updatePhi() {
        for (bb in functionData) {
            bb.phis { phi ->
                var changed = false
                bb.updateCF(phi) { oldBB, _ ->
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
        val newBlock = functionData.blocks.createBlock().apply {
            branch(bb)
        }
        p.updateCF(bb, newBlock)
        predecessorMap[p] = newBlock
    }

    companion object {
        fun run(module: Module): Module {
            module.functions().forEach { fnData ->
                SplitCriticalEdge(fnData).pass()
            }

            return module
        }
    }
}

fun Block.hasCriticalEdgeFrom(predecessor: Block): Boolean {
    return predecessor.successors().size > 1 && predecessors().size > 1
}