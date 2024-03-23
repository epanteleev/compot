package ir.pass.ana

import ir.dominance.DominatorTree
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label


data class LoopInfo(private val loopHeaders: Set<Block>) {
    fun isLoopHeader(bb: Label): Boolean {
        return loopHeaders.contains(bb)
    }

    fun headers(): Set<Block> = loopHeaders
}

class LoopDetection private constructor(val blocks: BasicBlocks, val dominatorTree: DominatorTree) {
    private fun evaluate(): LoopInfo {
        val loopHeaders = hashSetOf<Block>()
        for (bb in blocks.postorder()) {
            for (p in bb.predecessors()) {
                if (!dominatorTree.dominates(bb, p)) {
                    continue
                }

                loopHeaders.add(bb)
            }
        }

        return LoopInfo(loopHeaders)
    }

    companion object {
        fun evaluate(fd: FunctionData): LoopInfo {
            return LoopDetection(fd.blocks, fd.blocks.dominatorTree()).evaluate()
        }
    }
}