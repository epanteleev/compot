package ir.codegen.x64.regalloc.liveness

import ir.*
import ir.block.Block
import ir.utils.OrderedLocation

class Liveness private constructor(val data: FunctionData) {
    private val liveness = linkedMapOf<LocalValue, LiveRangeImpl>()
    private val bbOrdering: Map<Block, Int>

    init {
        setupArguments()
        bbOrdering = setupLiveRanges()
        evaluateUsages()
    }

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val begin = OrderedLocation(data.blocks.begin(), -(arguments.size - index))
            liveness[arg] = LiveRangeImpl(begin, begin)
        }
    }

    private fun setupLiveRanges(): Map<Block, Int> {
        val bbOrdering = hashMapOf<Block, Int>()
        var ordering = -1
        for (bb in data.blocks.linearScanOrder()) {
            for (inst in bb.instructions()) {
                ordering += 1
                if (inst !is LocalValue) {
                    continue
                }

                /** New definition. */
                val begin = OrderedLocation(bb, ordering)
                liveness[inst] = LiveRangeImpl(begin, begin)
            }
            bbOrdering[bb] = ordering
        }

        return bbOrdering
    }

    private fun evaluateUsages() {
        var ordering = -1
        for (bb in data.blocks.linearScanOrder()) {
            val maxBlock = evaluateMaxBlock(bb)

            for (inst in bb.instructions()) {
                ordering += 1

                val location = OrderedLocation(maxBlock, bbOrdering[maxBlock]!!)
                for (usage in inst.usages()) {
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

    private fun evaluateMaxBlock(bb: Block): Block {
        val predecessors = bb.predecessors()
        if (predecessors.size == 1) {
            return bb
        }

        val maxBlock = predecessors.fold(bb) { a, b ->
            if (bbOrdering[a]!! > bbOrdering[b]!!) {
                a
            } else {
                b
            }
        }

        return maxBlock
    }

    private fun sortedByCreation(): Map<LocalValue, LiveRange> {
        val pairList = liveness.toList().sortedBy { (_, value) -> value.begin().index }
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