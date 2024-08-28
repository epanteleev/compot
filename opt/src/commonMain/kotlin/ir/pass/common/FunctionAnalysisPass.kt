package ir.pass.common

import ir.module.MutationMarker


abstract class AnalysisResult(protected val marker: MutationMarker) {
    fun marker(): MutationMarker {
        return marker
    }
}

abstract class FunctionAnalysisPass<T: AnalysisResult> {
    abstract fun name(): String
    abstract fun run(): T
}

enum class AnalysisType {
    DOMINATOR_TREE,
    POST_DOMINATOR_TREE,
    LOOP_INFO,
    LIVENESS,
    LIVE_RANGE,
    ESCAPE_ANALYSIS,
    ALLOC_STORE_INFO,
    INTERFERENCE_GRAPH,
    JOIN_POINT_SET,
    LINEAR_SCAN_ORDER,
    LIVE_INTERVALS;

    companion object {
        fun size(): Int {
            return entries.size
        }
    }
}