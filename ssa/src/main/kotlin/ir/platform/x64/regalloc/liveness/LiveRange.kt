package ir.platform.x64.regalloc.liveness

import ir.utils.OrderedLocation


abstract class LiveRange internal constructor(private val creation: OrderedLocation, protected var end: OrderedLocation) {
    fun begin(): OrderedLocation = creation
    fun end(): OrderedLocation = end

    fun merge(other: LiveRange): LiveRange {
        val begin = minOf(other.creation, creation, compareBy { it.order })
        val end = maxOf(other.end, end, compareBy { it.order })
        return LiveRangeImpl(begin, end)
    }

    override fun toString(): String {
        return "range [$creation : $end]"
    }
}

class LiveRangeImpl internal constructor(creation: OrderedLocation, end: OrderedLocation): LiveRange(creation, end) {
    internal fun registerUsage(location: OrderedLocation) {
        if (end >= location) {
            return
        }

        end = location
    }
}