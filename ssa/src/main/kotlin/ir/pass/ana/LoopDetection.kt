package ir.pass.ana

import ir.module.BasicBlocks
import ir.module.block.Block
import ir.module.block.Label
import ir.dominance.DominatorTree


data class LoopBlockData(val header: Block, val loopBody: Set<Block>, private val exit: Block) {
    fun exit(): Block = exit

    fun enter(): Block {
        var enter: Block? = null
        for (s in header.successors()) {
            if (exit == s) {
                continue
            }

            enter = s
            break
        }
        return enter as Block
    }
}

data class LoopInfo(private val loopHeaders: Map<Block, LoopBlockData>) {
    fun get(bb: Label): LoopBlockData? {
        return loopHeaders[bb]
    }

    fun headers(): Set<Block> = loopHeaders.keys
}

class LoopDetection private constructor(val blocks: BasicBlocks, val dominatorTree: DominatorTree) {
    private fun evaluate(): LoopInfo {
        val loopHeaders = hashMapOf<Block, LoopBlockData>()
        for (bb in blocks.postorder()) {
            for (p in bb.predecessors()) {
                if (!dominatorTree.dominates(bb, p)) {
                    continue
                }

                val loopBody = getLoopBody(bb, p)
                val exit     = getExitBlock(bb, loopBody)

                loopHeaders[bb] = LoopBlockData(bb, loopBody, exit)
            }
        }

        return LoopInfo(loopHeaders)
    }

    private fun getExitBlock(header: Block, loopBody: Set<Block>): Block {
        for (s in header.successors()) {
            if (loopBody.contains(s)) {
                continue
            }
            return s
        }

        throw RuntimeException("unreachable")
    }

    private fun getLoopBody(header: Block, predecessor: Block): Set<Block> {
        val loopBody = mutableSetOf<Block>()
        val worklist = arrayListOf<Block>()
        worklist.add(predecessor)

        while (worklist.isNotEmpty()) {
            val bb = worklist.removeLast()
            if (bb == header) {
                continue
            }

            if (!loopBody.add(bb)) {
                continue
            }
            worklist.addAll(bb.predecessors())
        }

        return loopBody
    }

    companion object {
        fun evaluate(blocks: BasicBlocks): LoopInfo {
            return LoopDetection(blocks, blocks.dominatorTree()).evaluate()
        }
    }
}