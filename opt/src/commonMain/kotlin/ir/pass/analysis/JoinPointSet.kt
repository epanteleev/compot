package ir.pass.analysis

import common.intMapOf
import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.block.Label
import ir.module.FunctionData
import ir.module.block.AnyBlock
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric


class JoinPointSetResult internal constructor(private val joinSet: Map<AnyBlock, MutableSet<Alloc>>): AnalysisResult() {
    operator fun iterator(): Iterator<Map.Entry<AnyBlock, Set<Alloc>>> {
        return joinSet.iterator()
    }
}

class JoinPointSetEvaluate internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<JoinPointSetResult>() {
    private val frontiers = functionData.analysis(DominatorTreeFabric).frontiers()
    private val joinSet = intMapOf<AnyBlock, MutableSet<Alloc>>(functionData.size()) { bb: Label -> bb.index }

    private fun hasUserInBlock(bb: AnyBlock, variable: Alloc): Boolean {
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

    private fun calculateForVariable(v: Alloc, stores: MutableSet<AnyBlock>) {
        val phiPlaces = mutableSetOf<AnyBlock>()

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
            calculateForVariable(v, vStores as MutableSet<AnyBlock>)
        }

        return JoinPointSetResult(joinSet)
    }

    override fun name(): String {
        return "JoinPointSet"
    }

    override fun run(): JoinPointSetResult {
        return calculate()
    }
}

object JoinPointSetPassFabric: FunctionAnalysisPassFabric<JoinPointSetResult>() {
    override fun create(functionData: FunctionData): JoinPointSetEvaluate {
        return JoinPointSetEvaluate(functionData)
    }
}