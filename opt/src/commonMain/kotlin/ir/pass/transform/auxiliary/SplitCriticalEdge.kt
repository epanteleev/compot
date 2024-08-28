package ir.pass.transform.auxiliary

import ir.module.Module
import ir.module.FunctionData
import ir.module.block.Block


internal class SplitCriticalEdge private constructor(private val functionData: FunctionData) {
    private val predecessorMap = hashMapOf<Block, Block>()

    fun pass() {
        for (bbIdx in 0 until functionData.size()) {
            val predecessors = functionData[bbIdx].predecessors()
            for (index in predecessors.indices) {
                if (!functionData[bbIdx].hasCriticalEdgeFrom(predecessors[index])) {
                    continue
                }

                insertBasicBlock(functionData[bbIdx], predecessors[index])
            }
        }

        updatePhi()
    }

    private fun updatePhi() {
        for (bb in functionData) {
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
        val newBlock = functionData.blocks.createBlock().apply {
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