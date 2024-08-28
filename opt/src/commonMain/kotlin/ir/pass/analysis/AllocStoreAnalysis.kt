package ir.pass.analysis

import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.FunctionData
import ir.module.block.AnyBlock
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric
import ir.pass.isLocalVariable


class AllocAnalysisResult(private val storeInfo: Map<Alloc, Set<AnyBlock>>): AnalysisResult(), Iterable<Map.Entry<Alloc, Set<AnyBlock>>> {
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
        return AllocAnalysisResult(stores)
    }
}

object AllocStoreAnalysisFabric: FunctionAnalysisPassFabric<AllocAnalysisResult>() {
    override fun create(functionData: FunctionData): FunctionAnalysisPass<AllocAnalysisResult> {
        return AllocStoreAnalysis(functionData)
    }
}