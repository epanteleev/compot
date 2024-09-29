package ir.pass.analysis.intervals

import common.assertion

data class Location(val from: Int, val to: Int) {
    override fun toString(): String {
        return "[from=$from to=$to]"
    }

    fun merge(other: Location): Location {
        return Location(minOf(from, other.from), maxOf(to, other.to))
    }

    fun mergeTo(to: Int): Location {
        assertion(this.from <= to) {
            "Cannot merge to $to, current range is $this"
        }
        return if (this.to < to) {
            Location(from, to)
        } else {
            this
        }
    }

    fun intersect(other: Location): Boolean {
        if (from < other.from && other.from < to) {
            return true
        }
        if (from < other.to && other.to < to) {
            return true
        }

        return false
    }
}