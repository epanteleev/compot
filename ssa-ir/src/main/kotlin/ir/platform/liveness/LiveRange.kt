package ir.platform.liveness

import ir.module.block.Block
import ir.utils.OrderedLocation

open class LiveRange internal constructor(protected val creation: OrderedLocation, protected var end: OrderedLocation) {
    fun begin(): OrderedLocation {
        return creation
    }

    fun end(): OrderedLocation {
        return end
    }

    fun merge(other: LiveRange): LiveRange {
        val begin = minOf(other.creation, creation, compareBy { it.index })
        val end = maxOf(other.end, end, compareBy { it.index })
        return LiveRange(begin, end)
    }

    override fun toString(): String {
        return "range [$creation : $end]"
    }
}

class LiveRangeImpl internal constructor(creation: OrderedLocation, end: OrderedLocation): LiveRange(creation, end) {
    private fun addWork(worklist: MutableList<Block>, visited: MutableSet<Block>, bb: Block) {
        if (bb == creation.block) {
            return
        }

        for (p in bb.predecessors()) {
            if (visited.contains(p)) {
                continue
            }
            worklist.add(p)
        }
    }

    internal fun registerUsage(location: OrderedLocation, bbOrdering: Map<Block, Int>) {
        val worklist = arrayListOf<Block>()
        val visited  = mutableSetOf<Block>()
        addWork(worklist, visited, location.block)

        var currentKillerBlock = location.block

        while (worklist.isNotEmpty()) {
            val bb = worklist.removeLast()
            if (visited.contains(bb)) {
                continue
            }

            val ord = bbOrdering[bb]!!
            if (ord > bbOrdering[currentKillerBlock]!!) {
                currentKillerBlock = bb
                break
            }

            visited.add(bb)
            addWork(worklist, visited, bb)
        }

        worklist.clear()
        visited.clear()
        end = if (currentKillerBlock == location.block) {
            if (end >= location) {
                return
            }

            location
        } else {
            OrderedLocation(currentKillerBlock,
                currentKillerBlock.instructions().size - 1,
                bbOrdering[currentKillerBlock]!!)
        }
    }
}