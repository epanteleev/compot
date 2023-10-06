package ir.codegen.x64.regalloc.liveness

import ir.*
import ir.utils.OrderedLocation

class Liveness private constructor(val data: FunctionData) {
    private val liveness = linkedMapOf<LocalValue, LiveRangeImpl>()
    private val bbOrdering: Map<BasicBlock, Int>
    init {
        setupArguments()
        bbOrdering = setupLiveRanges()
        evaluateUsages()
    }

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val begin = OrderedLocation(data.blocks.begin(), -(arguments.size - index), -1)
            liveness[arg] = LiveRangeImpl(begin, begin)
        }
    }

    private fun setupLiveRanges(): Map<BasicBlock, Int> {
        val bbOrdering = hashMapOf<BasicBlock, Int>()
        var ordering = -1
        for (bb in data.blocks.linearScanOrder()) {
            for ((index, inst) in bb.instructions().withIndex()) {
                ordering += 1
                if (inst !is LocalValue) {
                    continue
                }

                /** New definition. */
                val begin = OrderedLocation(bb, ordering, index)
                liveness[inst] = LiveRangeImpl(begin, begin)
            }
            bbOrdering[bb] = ordering
        }

        return bbOrdering
    }

    private fun evaluateUsages() {
        var ordering = -1
        for (bb in data.blocks.linearScanOrder()) {
            val maxBlock = bb.predecessors().fold(bb) { a, b ->
                if (bbOrdering[a]!! > bbOrdering[b]!!) {
                    a
                } else {
                    b
                }
            }

            for ((index, inst) in bb.instructions().withIndex()) {
                ordering += 1

                val location = OrderedLocation(maxBlock, bbOrdering[maxBlock]!!, index)
                for (usage in inst.usedValues()) {
                    if (usage !is LocalValue) {
                        continue
                    }

                    val liveRange = liveness[usage]
                        ?: throw LiveIntervalsException("in $usage")

                    liveRange.registerUsage(location)
                }
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