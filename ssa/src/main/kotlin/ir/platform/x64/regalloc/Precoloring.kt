package ir.platform.x64.regalloc

import ir.LocalValue
import asm.x64.Operand
import ir.instruction.Phi
import ir.liveness.GroupedLiveIntervals
import ir.liveness.LiveRange
import ir.liveness.LiveIntervals


class Precoloring private constructor(private val intervals: LiveIntervals, private val precolored: Map<LocalValue, Operand>) {
    private val visited = hashSetOf<LocalValue>()
    private val liveness = hashMapOf<Group, LiveRange>()

    private fun build(): GroupedLiveIntervals {
        mergePhiOperands()
        completeOtherGroups()

        val result = liveness.toList().sortedBy { (_, value) -> value.begin().order } // TODO
        val map = linkedMapOf<Group, LiveRange>()
        for ((k, v) in result) {
            map[k] = v
        }

        return GroupedLiveIntervals(map)
    }

    private fun mergePhiOperands() {
        for ((value, range) in intervals) {
            if (value !is Phi) {
                continue
            }

            val group = arrayListOf<LocalValue>(value)
            visited.add(value)
            var liveRange = range
            for (used in value.usedInstructions()) {
                liveRange = liveRange.merge(intervals[used])
                group.add(used)
                visited.add(used)
            }

            liveness[Group(group, null)] = liveRange
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

            val op = precolored[value]
            liveness[Group(arrayListOf(value), op)] = range
            visited.add(value)
        }
    }

    companion object {
        fun evaluate(liveIntervals: LiveIntervals, registerMap: Map<LocalValue, Operand>): GroupedLiveIntervals {
            return Precoloring(liveIntervals, registerMap).build()
        }
    }
}