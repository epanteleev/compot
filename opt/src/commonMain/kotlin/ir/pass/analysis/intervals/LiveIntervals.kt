package ir.pass.analysis.intervals

import common.assertion
import ir.value.LocalValue
import ir.module.block.Label
import ir.pass.AnalysisResult
import ir.pass.analysis.LivenessAnalysisInfo
import ir.platform.x64.regalloc.Group

data class LiveIntervalsException(override val message: String): Exception(message)

class GroupedLiveIntervals(private val liveness: Map<Group, LiveRange>) {
    private val valueToGroup: Map<LocalValue, Group>

    init {
        valueToGroup = hashMapOf()
        for (group in liveness.keys) {
            for (value in group) {
                valueToGroup[value] = group
            }
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((group, range) in liveness) {
            builder.append("$group - $range\n")
        }

        return builder.toString()
    }

    operator fun get(v: LocalValue): LiveRange {
        return liveness[valueToGroup[v]]!!
    }

    operator fun get(v: Group): LiveRange {
        return liveness[v]!!
    }

    operator fun iterator(): Iterator<Map.Entry<Group, LiveRange>> {
        return liveness.iterator()
    }
}

class LiveIntervals(private val liveIntervals: Map<LocalValue, LiveRange>, private val liveness: LivenessAnalysisInfo): AnalysisResult() {
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

    fun liveOut(bb: Label): Set<LocalValue> {
        val info = liveness[bb]
        assertion(info != null) {
            "cannot find liveness information for $bb"
        }

        return info!!.liveOut()
    }

    operator fun iterator(): Iterator<Map.Entry<LocalValue, LiveRange>> {
        return liveIntervals.iterator()
    }
}