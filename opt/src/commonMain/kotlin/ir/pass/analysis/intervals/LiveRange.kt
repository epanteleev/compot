package ir.pass.analysis.intervals

import ir.module.block.Block


abstract class LiveRange internal constructor(protected var creation: Location, protected var locations: MutableMap<Block, Location>) {
    fun begin(): Location = creation
    fun end(): Location = locations.maxBy { it.value.order }.value //TODO cached

    fun intersect(other: LiveRange): Boolean {
        return !(creation > other.end() || other.creation > end()) //TODo not worried about holes
    }

    operator fun compareTo(other: LiveRange): Int {
        return creation.compareTo(other.creation)
    }

    override fun toString(): String {
        return "range [$creation : ${end()}]"
    }
}

class LiveRangeImpl internal constructor(creationBlock: Block, creation: Location): LiveRange(creation, hashMapOf(Pair(creationBlock, creation))) {
    fun merge(other: LiveRangeImpl) {
        if (creation > other.creation) {
            creation = other.creation
        }

        for ((block, location) in other.locations) {
            val loc = locations[block]
            if (loc == null || loc < location) {
                locations[block] = location
            }
        }
    }

    internal fun registerUsage(bb: Block, location: Location) {
        val loc = locations[bb]
        if (loc == null || loc < location) {
            locations[bb] = location
        }
    }
}