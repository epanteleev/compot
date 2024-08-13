package ir.pass.analysis

import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.module.block.AnyBlock
import ir.pass.common.AnalysisResult
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.isLocalVariable


class AllocAnalysisResult(private val storeInfo: Map<Alloc, Set<AnyBlock>>, marker: MutationMarker): AnalysisResult(marker),
    Iterable<Map.Entry<Alloc, Set<AnyBlock>>> {
    override fun iterator(): Iterator<Map.Entry<Alloc, Set<AnyBlock>>> {
        return storeInfo.iterator()
    }
}

class AllocStoreAnalysis internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<AllocAnalysisResult>() {
    private val stores: Map<Alloc, Set<AnyBlock>> by lazy { allStoresInternal() }

    override fun name(): String = "AllocStoreAnalysis"

    private inline fun forEachAlloc(closure: (Alloc) -> Unit) {
        for (bb in functionData) {
            for (inst in bb) {
                if (inst !is Alloc) {
                    continue
                }

                if (!inst.isLocalVariable()) {
                    continue
                }

                closure(inst)
            }
        }
    }

    private fun allStoresInternal(): Map<Alloc, Set<AnyBlock>> {
        val allStores = hashMapOf<Alloc, MutableSet<AnyBlock>>()
        forEachAlloc { alloc ->
            val stores = mutableSetOf<AnyBlock>()
            for (user in alloc.usedIn()) {
                if (user !is Store) {
                    continue
                }
                if (!user.isLocalVariable()) {
                    break
                }
                stores.add(user.owner())
            }
            allStores[alloc] = stores
        }

        return allStores
    }

    override fun run(): AllocAnalysisResult {
        return AllocAnalysisResult(stores, functionData.marker())
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