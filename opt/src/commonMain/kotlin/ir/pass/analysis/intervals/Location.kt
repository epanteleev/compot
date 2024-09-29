package ir.pass.analysis.intervals

data class Location(val index: Int, val order: Int) {
    override fun toString(): String {
        return "[$index order=$order]"
    }

    fun thisPlace(idx: Int): Boolean {
        return idx == index
    }

    operator fun compareTo(other: Location): Int {
        return order.compareTo(other.order)
    }
}