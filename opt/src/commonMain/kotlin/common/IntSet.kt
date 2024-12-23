package common


class IntSet<E>(private val bitmask: BooleanArray, val values: Array<E>, val closure: (E) -> Int): Set<E> {
    override val size: Int
        get() = bitmask.count { it }

    override fun isEmpty(): Boolean = values.all { it == null }

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

    override fun iterator(): Iterator<E> = IntSetIterator(bitmask, values)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IntSet<*>

        if (!bitmask.contentEquals(other.bitmask)) return false
        if (values.contentEquals(other.values)) return false
        if (closure != other.closure) return false

        return true
    }

    private class IntSetIterator<E>(private val bitmask: BooleanArray, val values: Array<E>) : Iterator<E> {
        private var current = 0

        override fun hasNext(): Boolean {
            while (current < bitmask.size && !bitmask[current]) {
                current++
            }

            return current < bitmask.size
        }

        override fun next(): E {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            return values[current++]
        }
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

inline fun <reified E> intSetOf(max: Int, vararg values: E, noinline closure: (E) -> Int): Set<E> {
    val boolArray = BooleanArray(max)
    val valArray = arrayOfNulls<E>(max)
    for (v in values) {
        val idx = closure(v)
        boolArray[idx] = true
        valArray[idx] = v
    }

    @Suppress("UNCHECKED_CAST")
    return IntSet(boolArray, valArray as Array<E>, closure)
}