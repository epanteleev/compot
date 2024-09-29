package ir.pass.analysis.intervals

data class Location(val order: Int) {
    override fun toString(): String {
        return "[order=$order]"
    }

    operator fun compareTo(other: Location): Int {
        return order.compareTo(other.order)
    }
}