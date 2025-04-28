package ir.pass.analysis

import ir.pass.common.*
import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.module.block.Block


class AllocAnalysisResult internal constructor(private val allocInfo: Map<Alloc, Set<Block>>, marker: MutationMarker): AnalysisResult(marker),
    Iterable<Map.Entry<Alloc, Set<Block>>> {
    override fun toString(): String = buildString {
        for ((alloc, stores) in allocInfo) {
            append("Alloc: $alloc\n")
            append("Stores: $stores\n")
        }
    }

    override fun iterator(): Iterator<Map.Entry<Alloc, Set<Block>>> {
        return allocInfo.iterator()
    }
}

private class AllocStoreAnalysis(private val functionData: FunctionData): FunctionAnalysisPass<AllocAnalysisResult>() {
    private val escapeState = functionData.analysis(EscapeAnalysisPassFabric)
    private val allocToBlock: Map<Alloc, Set<Block>> by lazy { allStoresInternal() }

    private inline fun forEachAlloc(closure: (Alloc) -> Unit) {
        for (bb in functionData) {
            for (inst in bb) {
                if (inst !is Alloc) {
                    continue
                }

                if (!escapeState.isNoEscape(inst)) {
                    continue
                }

                closure(inst)
            }
        }
    }

    private fun allStoresInternal(): Map<Alloc, Set<Block>> {
        val allStores = hashMapOf<Alloc, MutableSet<Block>>()
        forEachAlloc { alloc ->
            val stores = mutableSetOf<Block>()
            for (user in alloc.usedIn()) {
                if (user !is Store) {
                    continue
                }
                stores.add(user.owner())
            }
            allStores[alloc] = stores
        }

        return allStores
    }

    override fun run(): AllocAnalysisResult {
        return AllocAnalysisResult(allocToBlock, functionData.marker())
    }
}

object AllocStoreAnalysisFabric: FunctionAnalysisPassFabric<AllocAnalysisResult>() {
    override fun type(): AnalysisType {
        return AnalysisType.ALLOC_STORE_INFO
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.DATA_FLOW
    }

    override fun create(functionData: FunctionData): AllocAnalysisResult {
        return AllocStoreAnalysis(functionData).run()
    }
}