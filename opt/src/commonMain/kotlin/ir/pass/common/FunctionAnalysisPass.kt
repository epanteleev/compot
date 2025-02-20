package ir.pass.common

import ir.module.MutationMarker


abstract class AnalysisResult(protected val marker: MutationMarker) {
    abstract override fun toString(): String

    fun marker(): MutationMarker {
        return marker
    }
}

abstract class FunctionAnalysisPass<T: AnalysisResult> {
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
    JOIN_POINT_SET,
    LINEAR_SCAN_ORDER,
    LIVE_INTERVALS,
    POST_ORDER,
    PRE_ORDER,
    BACKWARD_POST_ORDER,
    BFS_ORDER,
    LINEAR_SCAN;

    companion object {
        fun size(): Int = entries.size
    }
}