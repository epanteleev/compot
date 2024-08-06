package ir.pass.analysis

import ir.module.block.Block
import ir.module.block.Label
import ir.pass.analysis.dominance.DominatorTree
import ir.module.FunctionData
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric


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

data class LoopInfo(private val loopHeaders: Map<Block, List<LoopBlockData>>): AnalysisResult() {
    val size: Int by lazy {
        loopHeaders.values.fold(0) { acc, list -> acc + list.size }
    }

    operator fun get(bb: Label): List<LoopBlockData>? = loopHeaders[bb]
    fun headers(): Set<Block> = loopHeaders.keys
}

class LoopDetection internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<LoopInfo>() {
    private val dominatorTree: DominatorTree = functionData.analysis(DominatorTreeFabric)

    private fun evaluate(): LoopInfo {
        val loopHeaders = hashMapOf<Block, List<LoopBlockData>>()
        for (bb in functionData.postorder()) {
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

    override fun name(): String {
        return "LoopDetection"
    }

    override fun run(): LoopInfo {
        return evaluate()
    }
}

object LoopDetectionPassFabric: FunctionAnalysisPassFabric<LoopInfo>() {
    override fun create(functionData: FunctionData): LoopDetection {
        return LoopDetection(functionData)
    }
}