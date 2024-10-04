package ir.pass.analysis.intervals

import common.assertion
import ir.instruction.Copy
import ir.value.LocalValue
import ir.module.FunctionData
import ir.instruction.Instruction
import ir.instruction.Phi
import ir.instruction.matching.aggregate
import ir.instruction.matching.gValue
import ir.instruction.matching.lea
import ir.module.Sensitivity
import ir.module.block.Block
import ir.module.block.Label
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.traverse.LinearScanOrderFabric
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.common.AnalysisType
import ir.platform.x64.pass.analysis.regalloc.Group
import ir.value.TupleValue


private class LiveIntervalsBuilder(private val data: FunctionData): FunctionAnalysisPass<LiveIntervals>() {
    private val intervals       = hashMapOf<LocalValue, LiveRangeImpl>()
    private val groups          = hashMapOf<LocalValue, Group>()
    private val dominatorTree   = data.analysis(DominatorTreeFabric)
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

    private val liveness        = data.analysis(LivenessAnalysisPassFabric)

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val loc = -(arguments.size - index)
            val begin = Location(loc, loc)
            intervals[arg] = LiveRangeImpl(data.begin(), begin)
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
                val begin = Location(ordering, ordering)
                intervals[inst] = LiveRangeImpl(bb, begin)
            }
        }
    }

    private fun updateLiveRange(inst: Instruction, bb: Block, instructionLocation: Int) {
        inst.operands { usage ->
            if (usage !is LocalValue) {
                return@operands
            }

            val liveRange = intervals[usage] ?: let {
                throw LiveIntervalsException("in $usage")
            }
            liveRange.registerUsage(bb, dominatorTree, linearScanOrder, instructionLocation)
        }
    }

    private fun evaluateUsages() {
        var ordering = -1
        for (bb in linearScanOrder.keys) {
            // TODO Improvement: skip this step if CFG doesn't have any loops.
            for (op in liveness.liveOut(bb)) {
                val liveRange = intervals[op] ?: throw LiveIntervalsException("cannot find $op")

                val index = bb.size
                liveRange.registerUsage(bb, dominatorTree, linearScanOrder, ordering + index)
            }

            for (inst in bb) {
                ordering += 1

                updateLiveRange(inst, bb, ordering)
            }
        }
    }

    private fun handlePhiOperands(phi: Phi, range: LiveRangeImpl) {
        val groupList = arrayListOf<LocalValue>(phi)
        phi.operands { used ->
            if (used !is LocalValue) {
                return@operands
            }
            used as Instruction
            assertion(used is Copy || lea(gValue(aggregate()))(used)) { "expect this invariant: used=$used" }

            range.merge(intervals[used]!!)
            intervals[used] = range
            groupList.add(used)
        }
        assertion(phi.usedIn().size == 1) {
            "phi=$phi, usedIn=${phi.usedIn()}"
        }

        val group = Group(groupList)
        phi.operands { used ->
            if (used !is LocalValue) {
                return@operands
            }
            groups[used] = group
        }
        groups[phi] = group
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

        return LiveIntervals(sortIntervals(), groups, data.marker())
    }

    private fun sortIntervals(): Map<LocalValue, LiveRange> {
        val sorted = intervals.toList().sortedBy { it.second.begin().from }
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