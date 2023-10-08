package ir.codegen.x64.regalloc

import ir.ArgumentValue
import ir.LocalValue
import ir.instruction.Phi
import ir.instruction.StackAlloc
import ir.codegen.x64.regalloc.liveness.LiveIntervals
import ir.codegen.x64.regalloc.liveness.LiveRange

data class Group(val values: List<LocalValue>) {
    val hasArgument: ArgumentValue? by lazy {
        values.find { it is ArgumentValue } as ArgumentValue?
    }

    val stackAllocGroup: Boolean by lazy {
        val isStackAlloc = values[0] is StackAlloc
        if (isStackAlloc) {
            assert( values.find { it !is StackAlloc } == null) {
                "must have only stackalloc values values=$values"
            }
        }

        isStackAlloc
    }

    fun first(): LocalValue {
        return values[0]
    }

    operator fun iterator(): Iterator<LocalValue> {
        return values.iterator()
    }

    override fun toString(): String {
        return values.joinToString(prefix = "[", separator = ",", postfix = "]")
    }
}

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

class Coalescing(private val intervals: LiveIntervals) {
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
            for (used in value.usedInstructions()) {
                liveRange = liveRange.merge(intervals[used])
                group.add(used)
                visited.add(used)
            }

            liveness[Group(group)] = liveRange
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

            liveness[Group(arrayListOf(value))] = range
            visited.add(value)
        }
    }

    companion object {
        fun evaluate(liveIntervals: LiveIntervals): CoalescedLiveIntervals {
            return Coalescing(liveIntervals).build()
        }
    }
}