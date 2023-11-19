package ir.platform.liveness

import ir.*
import ir.module.block.Block
import ir.instruction.Instruction
import ir.module.FunctionData
import ir.utils.OrderedLocation

class Liveness private constructor(val data: FunctionData) {
    private val liveness = linkedMapOf<LocalValue, LiveRangeImpl>()
    private val bbOrdering = hashMapOf<Block, Int>()

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

    private fun setupLiveRanges(): Map<Block, Int> {
        var ordering = -1
        for (bb in data.blocks.linearScanOrder()) {
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

        return bbOrdering
    }

    private fun evaluateUsages() {
        fun evaluateMaxBlock(bb: Block): Block {
            val predecessors = bb.predecessors()
            if (predecessors.size <= 1) {
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

        fun updateLiveRange(inst: Instruction, instructionLocation: OrderedLocation) {
            for (usage in inst.operands()) {
                if (usage !is LocalValue) {
                    continue
                }

                val liveRange = liveness[usage]
                    ?: throw LiveIntervalsException("in $usage")

                liveRange.registerUsage(instructionLocation)
            }
        }

        var ordering = -1
        for (bb in data.blocks.linearScanOrder()) {
            val maxBlock = evaluateMaxBlock(bb)
            for ((idx, inst) in bb.instructions().withIndex()) {
                ordering += 1
                val actualIndex = if (maxBlock != bb) {
                    bbOrdering[maxBlock]!!
                } else {
                    ordering
                }

                val location = OrderedLocation(maxBlock, idx, actualIndex)
                updateLiveRange(inst, location)
            }
        }
    }

    private fun doAnalysis(): LiveIntervals {
        fun sortedByCreation(): Map<LocalValue, LiveRange> {
            val pairList = liveness.toList().sortedBy { (_, value) -> value.begin().index }
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