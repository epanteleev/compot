package ir.codegen.x64.regalloc.liveness

import ir.*

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
        return liveness[v]!!
    }

    operator fun iterator(): Iterator<Map.Entry<LocalValue, LiveRange>> {
        return liveness.iterator()
    }
}