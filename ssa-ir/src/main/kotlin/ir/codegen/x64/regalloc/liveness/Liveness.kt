package ir.codegen.x64.regalloc.liveness

import ir.*
import ir.utils.OrderedLocation

class Liveness private constructor(val data: FunctionData) {
    private val liveness = linkedMapOf<LocalValue, LiveRangeImpl>()
    init {
        setupArguments()
        setupLiveRanges()
        evaluateUsages()
    }

    private fun setupArguments() {
        data.arguments().forEach {
            val begin = OrderedLocation(data.blocks.begin(), -1, -1)
            liveness[it] = LiveRangeImpl(begin, begin)
        }
    }

    private fun setupLiveRanges() {
        var ordering = 0
        for (bb in data.blocks.bfsTraversal()) {
            for ((index, inst) in bb.instructions().withIndex()) {
                if (inst !is LocalValue) {
                    continue
                }

                /** New definition. */
                val begin = OrderedLocation(bb, ordering, index)
                liveness[inst] = LiveRangeImpl(begin, begin)
                ordering += 1
            }
        }
    }

    private fun evaluateUsages() {
        var ordering = 0
        for (bb in data.blocks.linearScanOrder()) {
            for ((index, inst) in bb.instructions().withIndex()) {
                val location = OrderedLocation(bb, ordering, index)
                for (usage in inst.usedValues()) {
                    if (usage !is LocalValue) {
                        continue
                    }
                    val liveRange = liveness[usage]
                        ?: throw LiveIntervalsException("in $usage")

                    liveRange.registerUsage(location)
                }

                ordering += 1
            }
        }
    }

    private fun sortedByCreation(): Map<LocalValue, LiveRange> {
        val pairList = liveness.toList().sortedBy { (_, value) -> value.begin().order }
        val result = linkedMapOf<LocalValue, LiveRange>()
        for ((k, v) in pairList) {
            result[k] = v
        }

        return result
    }

    private fun doAnalysis(): LiveIntervals {
        return LiveIntervals(sortedByCreation())
    }

    companion object {
        fun evaluate(data: FunctionData): LiveIntervals {
            return Liveness(data).doAnalysis()
        }
    }
}