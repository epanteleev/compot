package ir.pass.analysis.intervals

data class Location(val from: Int, val to: Int) {
    override fun toString(): String {
        return "[from=$from to=$to]"
    }

    fun merge(other: Location): Location {
        return Location(minOf(from, other.from), maxOf(to, other.to))
    }
}