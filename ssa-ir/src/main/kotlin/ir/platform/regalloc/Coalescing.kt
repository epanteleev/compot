package ir.platform.regalloc

import asm.x64.Operand
import ir.LocalValue
import ir.instruction.Phi
import ir.platform.liveness.LiveIntervals
import ir.platform.liveness.LiveRange

class CoalescedLiveIntervals(private val liveness: Map<Group, LiveRange>) {
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

class Coalescing(private val intervals: LiveIntervals, private val precolored: Map<LocalValue, Operand>) {
    private val visited = hashSetOf<LocalValue>()
    private val liveness = hashMapOf<Group, LiveRange>()

    private fun build(): CoalescedLiveIntervals {
        coalescePhis()
        completeOtherGroups()

        val result = liveness.toList().sortedBy { (_, value) -> value.begin().index }
        val map = linkedMapOf<Group, LiveRange>()
        for ((k, v) in result) {
            map[k] = v
        }

        return CoalescedLiveIntervals(map)
    }

    private fun coalescePhis() {
        for ((value, range) in intervals) {
            if (value !is Phi) {
                continue
            }

            val group = arrayListOf<LocalValue>(value)
            visited.add(value)
            var liveRange = range
            var operand: Operand? = null
            for (used in value.usedInstructions()) {
                val op = precolored[used]
                operand = op

                liveRange = liveRange.merge(intervals[used])
                group.add(used)
                visited.add(used)
            }

            liveness[Group(group, operand)] = liveRange
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
        fun evaluate(liveIntervals: LiveIntervals, precolored: Map<LocalValue, Operand>): CoalescedLiveIntervals {
            return Coalescing(liveIntervals, precolored).build()
        }
    }
}