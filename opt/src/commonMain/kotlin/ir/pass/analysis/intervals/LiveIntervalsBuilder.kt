package ir.pass.analysis.intervals

import common.assertion
import ir.instruction.Copy
import ir.value.LocalValue
import ir.module.FunctionData
import ir.utils.OrderedLocation
import ir.instruction.Instruction
import ir.instruction.Phi
import ir.module.Sensitivity
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.traverse.LinearScanOrderFabric
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.common.AnalysisType
import ir.value.TupleValue


private class LiveIntervalsBuilder(private val data: FunctionData): FunctionAnalysisPass<LiveIntervals>() {
    private val intervals       = hashMapOf<LocalValue, LiveRangeImpl>()
    private val linearScanOrder = data.analysis(LinearScanOrderFabric)
    private val liveness        = data.analysis(LivenessAnalysisPassFabric)

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val begin = OrderedLocation(data.begin(), -1, -(arguments.size - index))
            intervals[arg] = LiveRangeImpl(begin)
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
                intervals[inst] = LiveRangeImpl(begin)
            }
        }
    }

    private fun updateLiveRange(inst: Instruction, instructionLocation: OrderedLocation) {
        inst.operands { usage ->
            if (usage !is LocalValue) {
                return@operands
            }

            val liveRange = intervals[usage] ?: let {
                throw LiveIntervalsException("in $usage")
            }
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

    private fun handlePhiOperands(value: Phi, range: LiveRangeImpl) {
        value.operands { used ->
            if (used !is LocalValue) {
                return@operands
            }
            assertion(used is Copy) { "expect this invariant: used=$used" }

            range.merge(intervals[used]!!)
            intervals[used] = range
        }
    }

    private fun handleTuple(value: TupleValue, range: LiveRangeImpl) {
        value.proj { proj ->
            range.merge(intervals[proj]!!)
            intervals[proj] = range
        }
    }

    private fun mergeInstructionIntervals() {
        for ((value, range) in intervals) {
            when (value) {
                is Phi -> handlePhiOperands(value, range)
                is TupleValue -> handleTuple(value, range)
            }
        }
    }

    override fun run(): LiveIntervals {
        setupArguments()
        setupLiveRanges()
        evaluateUsages()
        mergeInstructionIntervals()

        return LiveIntervals(sortIntervals(), data.marker())
    }

    private fun sortIntervals(): Map<LocalValue, LiveRange> {
        val sorted = intervals.toList().sortedBy { it.second.begin().order }
        val linkedMap = linkedMapOf<LocalValue, LiveRange>()
        for ((v, r) in sorted) {
            linkedMap[v] = r
        }

        return linkedMap
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