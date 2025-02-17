package ir.pass.analysis.intervals

import common.assertion
import ir.instruction.Copy
import ir.value.LocalValue
import ir.module.FunctionData
import ir.instruction.Instruction
import ir.instruction.Phi
import ir.instruction.isa
import ir.instruction.matching.lea
import ir.instruction.matching.any
import ir.module.Sensitivity
import ir.module.block.Block
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.traverse.LinearScanOrderFabric
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.common.AnalysisType
import ir.platform.x64.pass.analysis.regalloc.Group
import ir.value.TupleValue


private class LiveIntervalsBuilder(private val data: FunctionData): FunctionAnalysisPass<LiveIntervals>() {
    private val intervals       = hashMapOf<LocalValue, LiveRangeImpl>()
    private val groups          = hashMapOf<LocalValue, Group>()
    private val linearScanOrder = run {
        val linearScanOrder = data.analysis(LinearScanOrderFabric)
        val map = linkedMapOf<Block, Int>()
        var order = 0
        for (block in linearScanOrder) {
            map[block] = order
            order += block.size
        }
        map
    }

    private val liveness = data.analysis(LivenessAnalysisPassFabric)

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val loc = -(arguments.size - index)
            intervals[arg] = LiveRangeImpl(loc)
        }
    }

    private fun setupLiveRanges() {
        for ((bb, idx) in linearScanOrder) {
            var ordering = idx - 1
            for (inst in bb) {
                ordering += 1
                if (inst !is LocalValue) {
                    continue
                }

                /** New definition. */
                intervals[inst] = LiveRangeImpl(ordering)
            }
        }
    }

    private fun updateLiveRange(inst: Instruction, instructionLocation: Int) {
        for (usage in inst.operands()) {
            if (usage !is LocalValue) {
                continue
            }

            val liveRange = intervals[usage] ?: throw LiveIntervalsException("in $usage")
            liveRange.registerUsage(instructionLocation)
        }
    }

    private fun evaluateUsages() {
        var ordering = -1
        for (bb in linearScanOrder.keys) {
            // TODO Improvement: skip this step if CFG doesn't have any loops.
            for (op in liveness.liveOut(bb)) {
                val liveRange = intervals[op] ?: throw LiveIntervalsException("cannot find $op")

                val index = bb.size
                liveRange.registerUsage(ordering + index)
            }

            for (inst in bb) {
                ordering += 1

                updateLiveRange(inst, ordering)
            }
        }
    }

    private fun handlePhiOperands(phi: Phi, range: LiveRangeImpl) {
        val groupList = arrayListOf<LocalValue>(phi)
        for (used in phi.operands()) {
            if (used !is LocalValue) {
                continue
            }
            used as Instruction
            assertion(used is Copy || used.isa(lea(any()))) { "expect this invariant: used=$used" }

            range.merge(intervals[used]!!)
            intervals[used] = range
            groupList.add(used)
        }
        assertion(phi.usedIn().size == 1) {
            "phi=$phi, usedIn=${phi.usedIn()}"
        }

        val group = Group(groupList)
        for (used in phi.operands()) {
            if (used !is LocalValue) {
                continue
            }

            groups[used] = group
        }
        groups[phi] = group
    }

    private fun handleTuple(value: TupleValue, range: LiveRangeImpl) {
        value.proj { proj ->
            val projInterval = intervals[proj]!!
            projInterval.merge(range)
            intervals[proj] = projInterval
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

        return LiveIntervals(sortIntervals(), groups, data.marker())
    }

    private fun sortIntervals(): Map<LocalValue, LiveRange> {
        val sorted = intervals.toList().sortedBy { it.second.begin() }
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