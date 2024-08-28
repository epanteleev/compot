package ir.pass.analysis.intervals

import ir.value.LocalValue
import ir.module.FunctionData
import ir.utils.OrderedLocation
import ir.instruction.Instruction
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric
import ir.pass.analysis.LinearScanOrderFabric
import ir.pass.analysis.LivenessAnalysisPassFabric


class LiveIntervalsBuilder internal constructor(private val data: FunctionData): FunctionAnalysisPass<LiveIntervals>() {
    private val intervals       = linkedMapOf<LocalValue, LiveRangeImpl>()
    private val linearScanOrder = data.analysis(LinearScanOrderFabric)
    private val liveness        = data.analysis(LivenessAnalysisPassFabric)

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val begin = OrderedLocation(data.begin(), -1, -(arguments.size - index))
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
        inst.operands { usage ->
            if (usage !is LocalValue) {
                return@operands
            }

            val liveRange = intervals[usage] ?: throw LiveIntervalsException("in $usage")
            liveRange.registerUsage(instructionLocation)
        }
    }

    private fun evaluateUsages() {
        var ordering = -1
        for (bb in linearScanOrder) {
            // TODO Improvement: skip this step if CFG doesn't have any loops.
            for (op in liveness[bb].liveOut()) {
                val liveRange = intervals[op] ?: throw LiveIntervalsException("cannot find $op")

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

    override fun name(): String {
        return "LiveIntervals"
    }

    override fun run(): LiveIntervals {
        setupArguments()
        setupLiveRanges()
        evaluateUsages()
        return LiveIntervals(intervals)
    }
}

object LiveIntervalsFabric: FunctionAnalysisPassFabric<LiveIntervals>() {
    override fun create(functionData: FunctionData): FunctionAnalysisPass<LiveIntervals> {
        return LiveIntervalsBuilder(functionData)
    }
}