package ir.utils

import ir.*
import kotlin.math.max

data class Interval(val begin: Int, var end: Int)

class LiveRange(private val creation: Location) {
    private val intervals = hashMapOf<BasicBlock, Interval>()

    fun begin(): Location {
        return creation
    }

    private fun registerUsageInternal(location: Location): Boolean {
        val existedInterval = intervals[location.block]

        return if (existedInterval == null && location.block == creation.block) {
            intervals[location.block] = Interval(creation.index, location.index)
            false
        } else if (existedInterval == null) {
            intervals[location.block] = Interval(0, location.index)
            false
        } else {
            existedInterval.end = max(existedInterval.end, location.index)
            true
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

    fun isDiedHere(location: Location): Boolean {
        val interval = intervals[location.block] ?: return true
        return interval.end < location.index
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("creation $creation, live ")
        for ((bb, interval) in intervals) {
            builder.append("$bb[${interval.begin}:${interval.end}] ")
        }
        return builder.toString()
    }
}

class LiveIntervals(private val liveness: LinkedHashMap<LocalValue, LiveRange>) {
    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, ranges) in liveness) {
            builder.append("$v -> $ranges\n")
        }
        return builder.toString()
    }

    fun values(): Collection<LiveRange> {
        return liveness.values
    }

    operator fun get(v: LocalValue): LiveRange {
        return liveness[v]!!
    }

    operator fun iterator(): Iterator<Map.Entry<LocalValue, LiveRange>> {
        return liveness.iterator()
    }

    companion object {
        private fun evaluateForArguments(liveness: MutableMap<LocalValue, LiveRange>, data: FunctionData) {
            val loc = Location(data.blocks.begin(), 0)
            data.arguments().forEach { liveness[it] = LiveRange(loc) }
        }

        private fun evaluateForBasicBlock(liveness: MutableMap<LocalValue, LiveRange>, basicBlocks: BasicBlocks) {
            for (bb in basicBlocks.bfsTraversal()) {
                for ((index, inst) in bb.instructions.withIndex()) {
                    val location = Location(bb, index)

                    /** New definition. */
                    if (inst is ValueInstruction && inst.type() != Type.Void) {
                        liveness[inst] = LiveRange(location)
                    }

                    for (usage in inst.usages) {
                        if (usage !is LocalValue) {
                            continue
                        }
                        liveness[usage]!!.registerUsage(location)
                    }
                }
            }
        }
        fun evaluate(data: FunctionData): LiveIntervals {
            val liveness = linkedMapOf<LocalValue, LiveRange>()

            evaluateForArguments(liveness, data)
            evaluateForBasicBlock(liveness, data.blocks)
            return LiveIntervals(liveness)
        }
    }
}