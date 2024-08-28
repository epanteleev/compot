package ir.platform.x64.regalloc

import common.assertion
import ir.instruction.*
import ir.value.TupleValue
import ir.value.LocalValue
import ir.pass.analysis.intervals.LiveRange
import ir.pass.analysis.intervals.LiveIntervals
import ir.pass.analysis.intervals.GroupedLiveIntervals


//TODO
class Coalescing private constructor(private val intervals: LiveIntervals) {
    private val visited = hashSetOf<LocalValue>()
    private val groups = hashMapOf<Group, LiveRange>()

    private fun build(): GroupedLiveIntervals {
        mergePhiOperands()

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
        value.operands { used ->
            if (used !is LocalValue) {
                return@operands
            }
            assertion(used is Copy) { "expect this invariant: used=$used" }

            range.merge(intervals[used])
            group.add(used)
            intervals[used] = range
            visited.add(used)
        }
        groups[Group(group)] = range
    }

    private fun handleTuple(value: TupleValue, range: LiveRange) {
        visited.add(value)

        value.usedIn().forEach { proj ->
            proj as Projection
            if (visited.contains(proj)) {
                return@forEach
            }

            range.merge(intervals[proj])
            val group = Group(arrayListOf<LocalValue>(proj))
            groups[group] = range
            intervals[proj] = range

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

    companion object {
        fun evaluate(liveIntervals: LiveIntervals): GroupedLiveIntervals {
            return Coalescing(liveIntervals).build()
        }
    }
}