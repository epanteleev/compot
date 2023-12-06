package ir.platform.liveness

import ir.LocalValue

data class LiveIntervalsException(override val message: String): Exception(message)

class LiveIntervals(private val liveness: Map<LocalValue, LiveRange>) {
    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, ranges) in liveness) {
            builder.append("$v -> $ranges\n")
        }

        return builder.toString()
    }

    operator fun get(v: LocalValue): LiveRange {
        val range = liveness[v]
        assert(range != null) {
            "cannot find v=$v"
        }

        return range as LiveRange
    }

    operator fun iterator(): Iterator<Map.Entry<LocalValue, LiveRange>> {
        return liveness.iterator()
    }
}