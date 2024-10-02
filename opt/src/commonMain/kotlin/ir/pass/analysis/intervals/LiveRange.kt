package ir.pass.analysis.intervals

import ir.module.block.Block
import ir.pass.analysis.dominance.DominatorTree


abstract class LiveRange internal constructor(protected var locations: MutableMap<Block, Location>) {
    fun begin(): Location = locations.minBy { it.value.from }.value //TODO cached
    fun end(): Location = locations.maxBy { it.value.to }.value //TODO cached

    fun intersect(other: LiveRange): Boolean {
        return !(begin().from > other.end().to || other.begin().from > end().to) //TODo not worried about holes
        /*val maxRange = if (locations.size >= other.locations.size) {
            locations
        } else {
            other.locations
        }

        val minRange = if (locations.size >= other.locations.size) {
            other.locations
        } else {
            locations
        }

        for ((block, location) in maxRange) {
            val otherLocation = minRange[block] ?: continue
            if (location.intersect(otherLocation)) {
                return true
            }
        }

        return false*/
    }

    override fun toString(): String {
        return "range [${begin()} : ${end()}]"
    }
}

class LiveRangeImpl internal constructor(val creationBlock: Block, creation: Location): LiveRange(hashMapOf(Pair(creationBlock, creation))) {
    fun merge(other: LiveRangeImpl) {
        for ((block, location) in other.locations) {
            val loc = locations[block]
            if (loc == null) {
                locations[block] = location
            } else {
                locations[block] = loc.merge(location)
            }
        }
    }

    private fun propagateLocation(bb: Block, domTree: DominatorTree, locationMap: Map<Block, Int>) {
        if (bb == creationBlock) {
            val start = locationMap[bb]!!
            val loc = locations[bb]!!
            locations[bb] = loc.mergeTo(start + bb.size)
            return
        }

        for (dom in domTree.dominators(bb)) {
            dom as Block
            val start = locationMap[dom]!!
            val loc = locations[dom]
            if (loc != null) {
                locations[dom] = loc.mergeTo(start + dom.size)
                return
            } else {
                locations[dom] = Location(start, start + dom.size)
            }
        }
    }

    internal fun registerUsage(bb: Block, domTree: DominatorTree, locationMap: Map<Block, Int>, location: Int) {
        val loc = locations[bb]
        if (loc == null) {
            locations[bb] = Location(locationMap[bb]!!, location)
            propagateLocation(bb, domTree, locationMap)
        } else {
            locations[bb] = loc.mergeTo(location)
        }
    }
}