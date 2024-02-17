package ir.platform.x64.regalloc.liveness

import ir.LocalValue
import ir.instruction.Instruction
import ir.module.FunctionData
import ir.module.block.Block
import ir.utils.OrderedLocation


class Liveness private constructor(val data: FunctionData) {
    private val liveness = linkedMapOf<LocalValue, LiveRangeImpl>()
    private val bbOrdering = hashMapOf<Block, Int>()
    private val linearScanOrder = data.blocks.linearScanOrder().order()

    init {
        setupArguments()
        setupLiveRanges()
        evaluateUsages()
    }

    private fun setupArguments() {
        val arguments = data.arguments()
        for ((index, arg) in arguments.withIndex()) {
            val begin = OrderedLocation(data.blocks.begin(), -1, -(arguments.size - index))
            liveness[arg] = LiveRangeImpl(begin, begin)
        }
    }

    private fun setupLiveRanges() {
        var ordering = -1
        for (bb in linearScanOrder) {
            for ((idx, inst) in bb.instructions().withIndex()) {
                ordering += 1
                if (inst !is LocalValue) {
                    continue
                }

                /** New definition. */
                val begin = OrderedLocation(bb, idx, ordering)
                liveness[inst] = LiveRangeImpl(begin, begin)
            }
            bbOrdering[bb] = ordering
        }
    }

    private fun evaluateUsages() {
        fun updateLiveRange(inst: Instruction, instructionLocation: OrderedLocation) {
            for (usage in inst.operands()) {
                if (usage !is LocalValue) {
                    continue
                }

                val liveRange = liveness[usage]
                    ?: throw LiveIntervalsException("in $usage")

                liveRange.registerUsage(instructionLocation, bbOrdering)
            }
        }

        var ordering = -1
        for (bb in linearScanOrder) {
            for ((idx, inst) in bb.instructions().withIndex()) {
                ordering += 1
                val location = OrderedLocation(bb, idx, ordering)
                updateLiveRange(inst, location)
            }
        }
    }

    private fun doAnalysis(): LiveIntervals {
        fun sortedByCreation(): Map<LocalValue, LiveRange> {
            val pairList = liveness.toList().sortedBy { (_, value) -> value.begin().order }
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
            return Liveness(data).doAnalysis()
        }
    }
}