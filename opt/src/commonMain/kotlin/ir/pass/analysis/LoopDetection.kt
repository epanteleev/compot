package ir.pass.analysis

import ir.module.block.Block
import ir.module.block.Label
import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.pass.common.AnalysisResult
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.analysis.traverse.PostOrderFabric
import ir.pass.common.AnalysisType


data class LoopBlockData(val header: Block, val loopBody: Set<Block>, private val exit: Block) {
    fun exit(): Block = exit

    fun enter(): Block {
        var enter: Block? = null
        for (s in header.successors()) {
            if (exit == s) {
                continue
            }
            if (loopBody.contains(s)) {
                enter = s
                break
            }
        }
        return enter as Block
    }
}

class LoopInfo(private val loopHeaders: Map<Block, List<LoopBlockData>>, marker: MutationMarker): AnalysisResult(marker) {
    val size: Int by lazy {
        loopHeaders.values.fold(0) { acc, list -> acc + list.size }
    }

    operator fun get(bb: Label): List<LoopBlockData>? = loopHeaders[bb]
    fun headers(): Set<Block> = loopHeaders.keys
}

class LoopDetection internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<LoopInfo>() {
    private val dominatorTree = functionData.analysis(DominatorTreeFabric)
    private val postOrder     = functionData.analysis(PostOrderFabric)

    private fun evaluate(): LoopInfo {
        val loopHeaders = hashMapOf<Block, MutableList<LoopBlockData>>()
        for (bb in postOrder) {
            for (p in bb.predecessors()) {
                if (!dominatorTree.dominates(bb, p)) {
                    continue
                }

                val loopBody = getLoopBody(bb, p)
                val exit     = getExitBlock(bb, loopBody)

                loopHeaders.getOrPut(bb) { arrayListOf() }.add(LoopBlockData(bb, loopBody, exit))
            }
        }

        return LoopInfo(loopHeaders, functionData.marker())
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
    override fun type(): AnalysisType {
        return AnalysisType.LOOP_INFO
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): LoopInfo {
        return LoopDetection(functionData).run()
    }
}