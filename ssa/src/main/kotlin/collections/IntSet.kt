package collections


class IntSet<E>(private val bitmask: BooleanArray, val values: Array<E>, val closure: (E) -> Int): Set<E> {
    override val size: Int
        get() = bitmask.size

    override fun isEmpty(): Boolean = bitmask.isEmpty()

    override fun containsAll(elements: Collection<E>): Boolean {
        for (elem in elements) {
            if (!bitmask[closure(elem)]) {
                return false
            }
        }

        return true
    }

    override fun contains(element: E): Boolean {
        val idx = closure(element)
        return bitmask[idx] && values[idx] == element
    }

    override fun hashCode(): Int {
        return bitmask.hashCode()
    }

    override fun iterator(): Iterator<E> = values.iterator()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntSet<*>

        if (!bitmask.contentEquals(other.bitmask)) return false
        if (!values.contentEquals(other.values)) return false
        if (closure != other.closure) return false

        return true
    }
}

inline fun <reified E> intSetOf(values: Collection<E>, noinline closure: (E) -> Int): Set<E> {
    val array = arrayFrom(values)
    val boolArray = BooleanArray(values.size)
    for (v in array) {
        boolArray[closure(v)] = true
    }

    return IntSet(boolArray, array, closure)
}