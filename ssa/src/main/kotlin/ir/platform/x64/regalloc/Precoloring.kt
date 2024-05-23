package ir.platform.x64.regalloc

import ir.LocalValue
import asm.x64.Operand
import ir.instruction.Copy
import ir.instruction.Phi
import ir.instruction.Projection
import ir.instruction.TupleInstruction
import ir.liveness.GroupedLiveIntervals
import ir.liveness.LiveRange
import ir.liveness.LiveIntervals

//TODO
class Precoloring private constructor(private val intervals: LiveIntervals, private val precolored: Map<LocalValue, Operand>) {
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
            assert(used is Copy) { "expect this invariant: used=$used" }

            liveRange = liveRange.merge(intervals[used])
            group.add(used)
            visited.add(used)
        }

        groups[Group(group, null)] = liveRange
    }

    private fun handleTuple(value: TupleInstruction, range: LiveRange) {
        visited.add(value)
        for (i in value.usedIn().indices) {
            val group = arrayListOf<LocalValue>(value)
            value.proj(i) { proj ->
                if (visited.contains(proj)) {
                    return@proj
                }

                val op = precolored[proj]

                group.add(proj)
                groups[Group(group, op)] = range
                visited.add(proj)
            }
        }
    }

    private fun mergePhiOperands() {
        for ((value, range) in intervals) {
            when (value) {
                is Phi              -> handlePhiOperands(value, range)
                is TupleInstruction -> handleTuple(value, range)
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

            val op = precolored[value]
            groups[Group(arrayListOf(value), op)] = range
            visited.add(value)
        }
    }

    companion object {
        fun evaluate(liveIntervals: LiveIntervals, registerMap: Map<LocalValue, Operand>): GroupedLiveIntervals {
            return Precoloring(liveIntervals, registerMap).build()
        }
    }
}