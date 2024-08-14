package ir.pass.analysis.intervals

import common.assertion
import ir.module.MutationMarker
import ir.value.LocalValue
import ir.pass.common.AnalysisResult
import ir.platform.x64.regalloc.Group


data class LiveIntervalsException(override val message: String): Exception(message)

class MergedLiveIntervals(private val valueToGroup: Map<LocalValue, Group>) {
    fun getGroup(value: LocalValue): Group? {
        return valueToGroup[value]
    }
}

class LiveIntervals(private val liveIntervals: Map<LocalValue, LiveRange>, marker: MutationMarker): AnalysisResult(marker) {
    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, ranges) in liveIntervals) {
            builder.append("$v -> $ranges\n")
        }

        return builder.toString()
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