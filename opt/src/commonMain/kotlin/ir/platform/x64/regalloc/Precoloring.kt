package ir.platform.x64.regalloc

import common.assertion
import ir.instruction.*
import ir.value.TupleValue
import ir.value.LocalValue
import ir.pass.analysis.intervals.LiveRange
import ir.pass.analysis.intervals.LiveIntervals
import ir.pass.analysis.intervals.GroupedLiveIntervals


//TODO
class Precoloring private constructor(private val intervals: LiveIntervals) {
    private val visited = hashSetOf<LocalValue>()
    private val groups = hashMapOf<Group, LiveRange>()

    private fun build(): GroupedLiveIntervals {
        mergePhiOperands()
        completeOtherGroups()

        val result = groups.toList().sortedBy { (_, value) -> value.begin().order } // TODO
        val map = linkedMapOf<Group, LiveRange>()
        for ((k, v) in result) {
            map[k] = v
        }

        return GroupedLiveIntervals(map)
    }

    private fun handlePhiOperands(value: Phi, range: LiveRange) {
        val group = arrayListOf<LocalValue>(value)
        visited.add(value)
        var liveRange = range
        for (used in value.operands()) {
            if (used !is LocalValue) {
                continue
            }
            assertion(used is Copy) { "expect this invariant: used=$used" }

            liveRange = liveRange.merge(intervals[used])
            group.add(used)
            visited.add(used)
        }

        groups[Group(group)] = liveRange
    }

    private fun handleTuple(value: TupleValue, range: LiveRange) {
        visited.add(value)

        value.usedIn().forEach { proj ->
            proj as Projection
            if (visited.contains(proj)) {
                return@forEach
            }

            val liveRange = range.merge(intervals[proj])
            val group = Group(arrayListOf<LocalValue>(proj))
            groups[group] = liveRange

            visited.add(proj)
        }
    }

    private fun mergePhiOperands() {
        for ((value, range) in intervals) {
            when (value) {
                is Phi        -> handlePhiOperands(value, range)
                is TupleValue -> handleTuple(value, range)
            }
        }
    }

    private fun completeOtherGroups() {
        for ((value, range) in intervals) {
            if (value is Phi) {
                continue
            }
            if (visited.contains(value)) {
                continue
            }

            groups[Group(arrayListOf(value))] = range
            visited.add(value)
        }
    }

    companion object {
        fun evaluate(liveIntervals: LiveIntervals): GroupedLiveIntervals {
            return Precoloring(liveIntervals).build()
        }
    }
}