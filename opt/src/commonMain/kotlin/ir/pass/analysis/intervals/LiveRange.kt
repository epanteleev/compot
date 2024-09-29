package ir.pass.analysis.intervals

import ir.module.block.Block
import ir.pass.analysis.dominance.DominatorTree


abstract class LiveRange internal constructor(protected var locations: MutableMap<Block, Location>) {
    fun begin(): Location = locations.minBy { it.value.from }.value //TODO cached
    fun end(): Location = locations.maxBy { it.value.to }.value //TODO cached

    fun intersect(other: LiveRange): Boolean {
        return !(begin().from > other.end().to || other.begin().from > end().to) //TODo not worried about holes
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
        for (dom in domTree.dominators(bb)) {
            if (creationBlock == dom) {
                return
            }
            val start = locationMap[dom]!!
            locations[dom as Block] = Location(start, start + dom.size)
        }
    }

    internal fun registerUsage(bb: Block, domTree: DominatorTree, locationMap: Map<Block, Int>, location: Int) {
        val loc = locations[bb]
        if (loc == null) {
            locations[bb] = Location(location, location)
        } else {
            locations[bb] = loc.mergeTo(location)
        }
        if (bb == creationBlock) {
            return
        }
        propagateLocation(bb, domTree, locationMap)
    }
}