package ir.pass.analysis.intervals

import ir.module.block.Block
import ir.utils.OrderedLocation


abstract class LiveRange internal constructor(private var creation: OrderedLocation, protected var locations: MutableMap<Block, OrderedLocation>) {
    fun begin(): OrderedLocation = creation
    fun end(): OrderedLocation = locations.maxBy { it.value.order }.value //TODO cached

    fun merge(other: LiveRange) {
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

    fun intersect(other: LiveRange): Boolean {
        if (creation > other.end() || other.creation > end()) {
            return false
        }

        for ((block, location) in other.locations) {
            val loc = locations[block]
            if (loc != null && loc < location) {
                return true
            }
        }

        return false
    }

    override fun toString(): String {
        return "range [$creation : ${end()}]"
    }
}

class LiveRangeImpl internal constructor(creation: OrderedLocation, end: OrderedLocation): LiveRange(creation, hashMapOf(Pair(creation.block, end))) {
    internal fun registerUsage(location: OrderedLocation) {
        val loc = locations[location.block]
        if (loc == null || loc < location) {
            locations[location.block] = location
        }
    }
}