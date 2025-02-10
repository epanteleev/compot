package ir.pass.analysis.intervals


sealed class LiveRange(protected var creation: Int, protected var ending: Int) {
    fun begin(): Int = creation
    fun end(): Int = ending

    override fun hashCode(): Int {
        return creation * 31 + ending
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LiveRange

        if (creation != other.creation) return false
        if (ending != other.ending) return false

        return true
    }

    fun intersect(other: LiveRange): Boolean {
        if (other.begin() >= end()) {
            return false
        }
        if (begin() >= other.end()) {
            return false
        }

        return true
    }

    override fun toString(): String {
        return "range [${begin()} : ${end()}]"
    }
}

class LiveRangeImpl internal constructor(creation: Int): LiveRange(creation, creation) {
    fun merge(other: LiveRangeImpl) {
        creation = minOf(creation, other.creation)
        ending = maxOf(ending, other.ending)
    }

    internal fun registerUsage(location: Int) {
        ending = maxOf(ending, location)
    }
}