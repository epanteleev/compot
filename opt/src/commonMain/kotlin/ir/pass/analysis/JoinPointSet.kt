package ir.pass.analysis

import common.intMapOf
import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.block.Label
import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.module.block.Block
import ir.pass.common.AnalysisResult
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.common.AnalysisType


class JoinPointSetResult internal constructor(private val joinSet: Map<Block, MutableSet<Alloc>>, marker: MutationMarker) :
    AnalysisResult(marker) {
    override fun toString(): String = buildString {
        for ((block, allocs) in joinSet) {
            append("Block: $block\n")
            append("Allocs: $allocs\n")
        }
    }

    operator fun iterator(): Iterator<Map.Entry<Block, Set<Alloc>>> {
        return joinSet.iterator()
    }
}

private class JoinPointSetEvaluate(private val functionData: FunctionData) : FunctionAnalysisPass<JoinPointSetResult>() {
    private val frontiers = functionData.analysis(DominatorTreeFabric).frontiers()
    private val joinSet = intMapOf<Block, MutableSet<Alloc>>(functionData.size()) { bb: Label -> bb.index }
    private val liveness = functionData.analysis(LivenessAnalysisPassFabric)

    private fun hasUserInBlock(bb: Block, variable: Alloc): Boolean {
        if (bb === variable.owner()) {
            return true
        }
        for (users in variable.usedIn()) {
            if (users !is Store) {
                continue
            }
            if (bb === users.owner()) {
                return true
            }
        }
        return false
    }

    private fun calculateForVariable(v: Alloc, stores: MutableSet<Block>) {
        val phiPlaces = mutableSetOf<Block>()

        while (stores.isNotEmpty()) {
            val x = stores.first()
            stores.remove(x)

            if (!frontiers.contains(x)) {
                continue
            }

            for (frontier in frontiers[x]!!) {
                if (phiPlaces.contains(frontier)) {
                    continue
                }
                if (!liveness.liveIn(frontier).contains(v)) { //TODO: there is a better way to check live-in
                    continue
                }

                val values = joinSet.getOrPut(frontier) { mutableSetOf() }

                values.add(v)
                phiPlaces.add(frontier)

                if (!hasUserInBlock(frontier, v)) {
                    stores.add(frontier)
                }
            }
        }
    }

    private fun calculate(): JoinPointSetResult {
        val allocInfo = functionData.analysis(AllocStoreAnalysisFabric)

        for ((v, vStores) in allocInfo) {
            calculateForVariable(v, vStores as MutableSet<Block>)
        }

        return JoinPointSetResult(joinSet, functionData.marker())
    }

    override fun run(): JoinPointSetResult {
        return calculate()
    }
}

object JoinPointSetPassFabric : FunctionAnalysisPassFabric<JoinPointSetResult>() {
    override fun type(): AnalysisType {
        return AnalysisType.JOIN_POINT_SET
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): JoinPointSetResult {
        return JoinPointSetEvaluate(functionData).run()
    }
}