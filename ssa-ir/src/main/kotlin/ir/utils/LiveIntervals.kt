package ir.utils

import ir.*
import kotlin.math.max

data class LiveIntervalsException(override val message: String): Exception(message)

data class Interval(val begin: Int, var end: Int)

class LiveRange(private val creation: Location) {
    private val intervals = hashMapOf<BasicBlock, Interval>()

    fun begin(): Location {
        return creation
    }

    private fun registerUsageInternal(location: Location) {
        val existedInterval = intervals[location.block]

        if (existedInterval == null && location.block == creation.block) {
            intervals[location.block] = Interval(creation.index, location.index)
        } else if (existedInterval == null) {
            intervals[location.block] = Interval(0, location.index)
        } else {
            existedInterval.end = max(existedInterval.end, location.index)
        }
    }

    private fun propagate(location: Location) {
        val worklist = arrayListOf(location.block)
        val visited = hashSetOf<BasicBlock>()

        fun iter(block: BasicBlock) {
            for (predecessor in block.predecessors) {
                registerUsageInternal(Location(predecessor, predecessor.instructions.size))
                if (!visited.contains(predecessor) && predecessor != creation.block) {
                    worklist.add(predecessor)
                }
            }
            visited.add(block)
        }

        while (worklist.isNotEmpty()) {
            val block = worklist.removeAt(worklist.size - 1)
            iter(block)
        }
    }

    fun registerUsage(location: Location) {
        registerUsageInternal(location)
        if (location.block != creation.block) {
            propagate(location)
        }
    }

    fun registerUsageInPhi(location: Location, incoming: BasicBlock) {
        registerUsageInternal(location)
        registerUsageInternal(Location(incoming, incoming.size))
        if (incoming != creation.block) {
            propagate(Location(incoming, incoming.size))
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
        private fun argumentsLiveness(liveness: MutableMap<LocalValue, LiveRange>, data: FunctionData) {
            val loc = Location(data.blocks.begin(), 0)
            data.arguments().forEach { liveness[it] = LiveRange(loc) }
        }

        private fun setupLiveRanges(liveness: MutableMap<LocalValue, LiveRange>, basicBlocks: BasicBlocks) {
            for (bb in basicBlocks) {
                for ((index, inst) in bb.instructions.withIndex()) {
                    if (inst !is LocalValue) {
                        continue
                    }
                    val location = Location(bb, index)
                    /** New definition. */
                    liveness[inst] = LiveRange(location)
                }
            }
        }
        private fun evaluateUsages(liveness: MutableMap<LocalValue, LiveRange>, basicBlocks: BasicBlocks) {
            for (bb in basicBlocks.bfsTraversal()) {
                for ((index, inst) in bb.instructions.withIndex()) {
                    val location = Location(bb, index)

                    if (inst is Phi) {
                        for ((usage, bb) in inst.usedValues().zip(inst.incoming())) {
                            if (usage !is LocalValue) {
                                continue
                            }
                            val liveRange = liveness[usage]
                                ?: throw LiveIntervalsException("in $usage")

                            liveRange.registerUsageInPhi(location, bb as BasicBlock)
                        }
                    } else {
                        for (usage in inst.usedValues()) {
                            if (usage !is LocalValue) {
                                continue
                            }
                            val liveRange = liveness[usage]
                                ?: throw LiveIntervalsException("in $usage")

                            liveRange.registerUsage(location)
                        }
                    }


                }
            }
        }

        fun evaluate(data: FunctionData): LiveIntervals {
            val liveness = linkedMapOf<LocalValue, LiveRange>()

            argumentsLiveness(liveness, data)
            setupLiveRanges(liveness, data.blocks)
            evaluateUsages(liveness, data.blocks)
            return LiveIntervals(liveness)
        }
    }
}