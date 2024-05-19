package ir.liveness

import ir.LocalValue
import ir.module.FunctionData
import ir.utils.OrderedLocation
import ir.instruction.Instruction


class LiveIntervalsBuilder private constructor(val data: FunctionData) {
    private val intervals = linkedMapOf<LocalValue, LiveRangeImpl>()
    private val loopInfo = data.blocks.loopInfo()
    private val linearScanOrder = data.blocks.linearScanOrder(loopInfo).order()
    private val liveness = LivenessAnalysis.evaluate(data, linearScanOrder)

    init {
        setupArguments()
        setupLiveRanges()
        evaluateUsages()
    }

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val begin = OrderedLocation(data.blocks.begin(), -1, -(arguments.size - index))
            intervals[arg] = LiveRangeImpl(begin, begin)
        }
    }

    private fun setupLiveRanges() {
        var ordering = -1
        for (bb in linearScanOrder) {
            for ((idx, inst) in bb.withIndex()) {
                ordering += 1
                if (inst !is LocalValue) {
                    continue
                }

                /** New definition. */
                val begin = OrderedLocation(bb, idx, ordering)
                intervals[inst] = LiveRangeImpl(begin, begin)
            }
        }
    }

    private fun updateLiveRange(inst: Instruction, instructionLocation: OrderedLocation) {
        for (usage in inst.operands()) {
            if (usage !is LocalValue) {
                continue
            }

            val liveRange = intervals[usage]
            if (liveRange == null) {
                throw LiveIntervalsException("in $usage")
            }

            liveRange.registerUsage(instructionLocation)
        }
    }

    private fun evaluateUsages() {
        var ordering = -1
        for (bb in linearScanOrder) {
            // TODO Improvement: skip this step if CFG doesn't have any loops.
            for (op in liveness[bb]!!.liveOut()) {
                val liveRange = intervals[op]
                if (liveRange == null) {
                    throw LiveIntervalsException("cannot find $op")
                }

                val index = bb.size + 1
                liveRange.registerUsage(OrderedLocation(bb, index, ordering + index))
            }

            for ((idx, inst) in bb.withIndex()) {
                ordering += 1

                val location = OrderedLocation(bb, idx, ordering)
                updateLiveRange(inst, location)
            }
        }
    }

    private fun doAnalysis(): LiveIntervals {
        fun sortedByCreation(): Map<LocalValue, LiveRange> {
            val pairList = intervals.toList().sortedBy { (_, value) -> value.begin().order }
            val result = linkedMapOf<LocalValue, LiveRange>()
            for ((k, v) in pairList) {
                result[k] = v
            }

            return result
        }

        return LiveIntervals(sortedByCreation())
    }

    companion object {
        fun evaluate(data: FunctionData): LiveIntervals {
            return LiveIntervalsBuilder(data).doAnalysis()
        }
    }
}