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

data class LoopInfo(private val loopHeaders: Map<Block, List<LoopBlockData>>) {
    val size: Int by lazy {
        loopHeaders.values.fold(0) { acc, list -> acc + list.size }
    }

    operator fun get(bb: Label): List<LoopBlockData>? = loopHeaders[bb]
    fun headers(): Set<Block> = loopHeaders.keys
}

class LoopDetection private constructor(val blocks: BasicBlocks, val dominatorTree: DominatorTree) {
    private fun evaluate(): LoopInfo {
        val loopHeaders = hashMapOf<Block, List<LoopBlockData>>()
        for (bb in blocks.postorder()) {
            for (p in bb.predecessors()) {
                if (!dominatorTree.dominates(bb, p)) {
                    continue
                }

                val loopBody = getLoopBody(bb, p)
                val exit     = getExitBlock(bb, loopBody)

                loopHeaders[bb] = loopHeaders.getOrElse(bb) { emptyList() } + LoopBlockData(bb, loopBody, exit)
            }
        }

        return LoopInfo(loopHeaders)
    }

    private fun getExitBlock(header: Block, loopBody: Set<Block>): Block {
        for (l in loopBody) {
            for (s in l.successors()) {
                if (!loopBody.contains(s)) {
                    return s
                }
            }
        }

        throw RuntimeException("unreachable, header=$header, loopBody=$loopBody")
    }

    private fun getLoopBody(header: Block, predecessor: Block): Set<Block> {
        val loopBody = mutableSetOf(header)
        val worklist = arrayListOf<Block>()
        worklist.add(predecessor)

        while (worklist.isNotEmpty()) {
            val bb = worklist.removeLast()
            if (bb == header) {
                continue
            }

            if (!dominatorTree.dominates(header, bb)) {
                // Isn't in the loop. Skip
                continue
            }

            if (!loopBody.add(bb)) {
                // Already inserted. Skip
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