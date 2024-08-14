package ir.pass.analysis.intervals

import common.assertion
import ir.value.LocalValue
import ir.module.FunctionData
import ir.utils.OrderedLocation
import ir.instruction.Instruction
import ir.module.Sensitivity
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.traverse.LinearScanOrderFabric
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.common.AnalysisType


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
            for (op in liveness.liveOut(bb)) {
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

        checkConsistency()
        return LiveIntervals(intervals, data.marker())
    }

    /**
     * Check that all live ranges are in order by their start.
     */
    private fun checkConsistency() {
        var current: Int = Int.MIN_VALUE
        for ((_, range) in intervals) {
            assertion(range.begin().order >= current) {
                "current=${range.begin().order} expected=${current}"
            }
            current = range.begin().order
        }
    }
}

object LiveIntervalsFabric: FunctionAnalysisPassFabric<LiveIntervals>() {
    override fun type(): AnalysisType {
        return AnalysisType.LIVE_INTERVALS
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): LiveIntervals {
        return LiveIntervalsBuilder(functionData).run()
    }
}