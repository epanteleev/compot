package ir.pass.transform.auxiliary

import ir.instruction.Branch
import ir.module.Module
import ir.module.FunctionData
import ir.module.block.Block


internal class SplitCriticalEdge private constructor(private val functionData: FunctionData) {
    fun pass() {
        val criticalEdgeBetween = hashMapOf<Block, MutableList<Block>>()
        for (bb in functionData) {
            for (p in bb.predecessors()) {
                if (!bb.hasCriticalEdgeFrom(p)) {
                    continue
                }

                val successors = criticalEdgeBetween.getOrPut(p) { arrayListOf() }
                successors.add(bb)
            }
        }

        for ((p, bbs) in criticalEdgeBetween) {
            for (b in bbs) {
                insertBasicBlock(b, p)
            }
        }
    }

    private fun insertBasicBlock(bb: Block, p: Block) {
        val newBlock = functionData.blocks.createBlock()
        newBlock.put(Branch.br(bb))

        p.updateCF(bb, newBlock)
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

internal fun Block.hasCriticalEdgeFrom(predecessor: Block): Boolean {
    return predecessor.successors().size > 1 && predecessors().size > 1
}