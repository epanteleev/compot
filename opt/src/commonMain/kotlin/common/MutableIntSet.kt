package common


class MutableIntSet<E>(private val bitmask: BooleanArray, val values: Array<E?>, val closure: (E) -> Int): MutableSet<E> {
    override val size: Int
        get() = bitmask.count { it }

    override fun clear() {
        bitmask.fill(false)
        values.fill(null)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        var isModified = false
        for (elem in elements) {
            isModified = isModified or add(elem)
        }

        return isModified
    }

    override fun add(element: E): Boolean {
        val idx = closure(element)
        val isHas = bitmask[idx]
        bitmask[idx] = true
        values[idx] = element
        return isHas
    }

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
        assertion(values.contains(element)) {
            "cannot find element=$element"
        }

        return bitmask[closure(element)]
    }

    override fun iterator(): MutableIterator<E> {
        val ret = arrayListOf<E>()
        for (el in values) {
            if (el == null) {
                continue
            }

            ret.add(el)
        }

        return ret.iterator()
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var isModified = false
        for (elem in elements) {
            if (elements.contains(elem)) {
                remove(elem)
                isModified = true
            }
        }

        return isModified
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var isModified = false
        for (elem in elements) {
            remove(elem)
            isModified = true
        }

        return isModified
    }

    override fun remove(element: E): Boolean {
        val idx = closure(element)
        val isHas = bitmask[idx]
        bitmask[idx] = false
        values[idx] = null
        return isHas
    }
}

inline fun <reified E> mutableIntSetOf(values: Collection<E>, noinline closure: (E) -> Int): Set<E> {
    val array = values.toTypedArray()
    val boolArray = BooleanArray(values.size)
    for (v in array) {
        boolArray[closure(v)] = true
    }

    @Suppress("UNCHECKED_CAST")
    return MutableIntSet(boolArray, array as Array<E?>, closure)
}

inline fun <reified E> mutableIntSetOf(size: Int, noinline closure: (E) -> Int): Set<E> {
    return MutableIntSet(BooleanArray(size), arrayOfNulls<E>(size), closure)
}