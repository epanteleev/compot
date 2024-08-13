package ir.pass

import common.intMapOf
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.pass.common.AnalysisResult
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPassFabric

/**
 * Cache for analysis pass result.
 */
class AnalysisPassCache {
    private val cache by lazy { intMapOf<FunctionAnalysisPassFabric<AnalysisResult>, AnalysisResult>(AnalysisType.size()) { it.type().ordinal } }
    private var cfMutationMarker = 0
    private var dfMutationCount = 0

    fun getResult(type: FunctionAnalysisPassFabric<AnalysisResult>, mutationMarker: MutationMarker): AnalysisResult? {
        val result = cache[type] ?: return null
        val mutationType = mutationMarker.mutationType(result.marker()) ?: return result
        if (mutationType.isIntersection(type.sensitivity())) {
            cache.remove(type)
            return null
        }

        return result
    }

    inline fun <reified T: AnalysisResult> get(type: FunctionAnalysisPassFabric<T>, mutationMarker: MutationMarker): T? {
        val result = getResult(type, mutationMarker)
        return if (result is T) result else null
    }

    fun<T: AnalysisResult> put(key: FunctionAnalysisPassFabric<T>, value: T): T {
        cache[key] = value
        return value
    }
}