package ir.pass.analysis.intervals

import ir.module.block.Block
import ir.utils.OrderedLocation


abstract class LiveRange internal constructor(protected var creation: OrderedLocation, protected var locations: MutableMap<Block, OrderedLocation>) {
    fun begin(): OrderedLocation = creation
    fun end(): OrderedLocation = locations.maxBy { it.value.order }.value //TODO cached

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

class LiveRangeImpl internal constructor(creation: OrderedLocation): LiveRange(creation, hashMapOf(Pair(creation.block, creation))) {
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

    internal fun registerUsage(location: OrderedLocation) {
        val loc = locations[location.block]
        if (loc == null || loc < location) {
            locations[location.block] = location
        }
    }
}