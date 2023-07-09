package ir.utils

import ir.*
import kotlin.math.max

data class Interval(val begin: Int, var end: Int)

class LiveRange(private var creation: Location) {
    private val intervals = hashMapOf<BasicBlock, Interval>()

    private fun registerUsageInternal(location: Location): Boolean {
        val existedInterval = intervals[location.block]

        if (existedInterval == null && location.block == creation.block) {
            intervals[location.block] = Interval(creation.index, location.index)
            return false
        } else if (existedInterval == null) {
            intervals[location.block] = Interval(0, location.index)
            return false
        } else {
            existedInterval.end = max(existedInterval.end, location.index)
            return true
        }
    }

    private fun propagate(location: Location) {
        val worklist = arrayListOf(location.block)

        fun iter(block: BasicBlock) {
            for (predecessor in block.predecessors) {
                if (registerUsageInternal(Location(predecessor, predecessor.instructions.size))) {
                    worklist.add(predecessor)
                }
            }
        }

        while (worklist.isNotEmpty()) {
            val block = worklist.removeAt(worklist.size - 1)
            if (block.equals(Label.entry)) {
                continue
            }
            iter(block)
        }
    }

    fun registerUsage(location: Location) {
        val needPropagateLiveness = registerUsageInternal(location)
        if (needPropagateLiveness) {
            propagate(location)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("creation $creation, live ")
        for ((bb, interval) in intervals) {
            builder.append("$bb[${interval.begin}:${interval.begin}] ")
        }
        return builder.toString()
    }
}

class LiveIntervals(private val liveness: Map<Value, LiveRange>) {
    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, ranges) in liveness) {
            builder.append("$v -> $ranges\n")
        }
        return builder.toString()
    }

    operator fun iterator(): Iterator<Map.Entry<Value, LiveRange>> {
        return liveness.iterator()
    }

    companion object {
        fun evaluate(basicBlocks: BasicBlocks): LiveIntervals {
            val liveness = hashMapOf<Value, LiveRange>()

            for (bb in basicBlocks.preorder()) {
                for ((index, inst) in bb.instructions.withIndex()) {
                    val location = Location(bb, index)

                    if (inst !is TerminateInstruction && inst !is Store) {
                        liveness[inst] = LiveRange(location)
                    }

                    for (usage in inst.usages) {
                        if (usage !is Instruction) {
                            continue
                        }
                        liveness[usage]!!.registerUsage(location)
                    }
                }
            }
            return LiveIntervals(liveness)
        }
    }
}