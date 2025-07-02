package ir.pass.analysis.intervals

import common.assertion
import ir.module.MutationMarker
import ir.value.LocalValue
import ir.pass.common.AnalysisResult
import ir.platform.x64.pass.analysis.regalloc.Group


data class LiveIntervalsException(override val message: String): Exception(message)

class LiveIntervals(private val liveIntervals: Map<LocalValue, LiveRange>, private val valueToGroup: Map<LocalValue, Group>, marker: MutationMarker): AnalysisResult(marker) {
    override fun toString(): String = buildString {
        for ((v, ranges) in liveIntervals) {
            append("$v -> $ranges\n")
        }
    }

    fun getGroup(value: LocalValue): Group? {
        return valueToGroup[value]
    }

    operator fun get(v: LocalValue): LiveRange {
        val range = liveIntervals[v]
        assertion(range != null) {
            "cannot find v=$v"
        }

        return range as LiveRange
    }

    operator fun iterator(): Iterator<Map.Entry<LocalValue, LiveRange>> {
        return liveIntervals.iterator()
    }
}