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


data class LoopBlockData(private val header: Block, private val loopBody: Set<Block>, private val exit: Block, private val enter: Block) {
    fun exit(): Block = exit
    fun enter(): Block = enter
}

class LoopInfo(private val loopHeaders: Map<Block, List<LoopBlockData>>, marker: MutationMarker) : AnalysisResult(marker) {
    val size: Int by lazy {
        loopHeaders.values.fold(0) { acc, list -> acc + list.size }
    }

    override fun toString(): String = buildString {
        for ((header, loops) in loopHeaders) {
            append("Header: $header\n")
            for (loop in loops) {
                append("Loop: $loop\n")
            }
        }
    }

    operator fun get(bb: Label): List<LoopBlockData>? = loopHeaders[bb]
    fun headers(): Set<Block> = loopHeaders.keys
}

private class LoopDetection(private val functionData: FunctionData) : FunctionAnalysisPass<LoopInfo>() {
    private val dominatorTree = functionData.analysis(DominatorTreeFabric)
    private val postOrder     = functionData.analysis(PostOrderFabric)

    private fun findExit(header: Block, loopBody: Set<Block>): Block {
        for (l in loopBody) {
            for (s in l.successors()) {
                if (!loopBody.contains(s)) {
                    return s
                }
            }
        }

        throw RuntimeException("unreachable, header=$header, loopBody=$loopBody")
    }

    private fun findEnter(header: Block, loopBody: Set<Block>): Block {
        for (s in header.successors()) {
            if (loopBody.contains(s)) {
                return s
            }
        }

        throw RuntimeException("unreachable, header=$header, loopBody=$loopBody")
    }

    private fun makeLoopBlockData(header: Block, loopBody: Set<Block>): LoopBlockData {
        val exit = findExit(header, loopBody)
        val enter = findEnter(header, loopBody)

        return LoopBlockData(header, loopBody, exit, enter)
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

    override fun run(): LoopInfo {
        val loopHeaders = hashMapOf<Block, MutableList<LoopBlockData>>()
        for (bb in postOrder) {
            for (p in bb.predecessors()) {
                if (!dominatorTree.dominates(bb, p)) {
                    continue
                }

                val loopBody = getLoopBody(bb, p)
                val loopData = makeLoopBlockData(bb, loopBody)

                loopHeaders.getOrPut(bb) { arrayListOf() }.add(loopData)
            }
        }

        return LoopInfo(loopHeaders, functionData.marker())
    }
}

object LoopDetectionPassFabric : FunctionAnalysisPassFabric<LoopInfo>() {
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