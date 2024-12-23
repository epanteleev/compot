package common

class LinkedIntSet<E>(private val bitmask: BooleanArray, private val values: Array<Node<E>?>, val closure: (E) -> Int): Set<E> {
    override val size: Int
        get() = bitmask.count { it }

    fun clear() {
        values.fill(null)
        bitmask.fill(false)
    }

    fun addAll(elements: Collection<E>): Boolean {
        var changed = false
        for (elem in elements) {
            val idx = closure(elem)
            if (!bitmask[idx]) {
                bitmask[idx] = true
                values[idx] = Node(null, null, elem)
                changed = true
            }
        }

        return changed
    }

    fun add(element: E): Boolean {
        TODO("Not yet implemented")
    }

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

    override fun iterator(): MutableIterator<E> = TODO()
    fun retainAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    fun removeAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    fun remove(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LinkedIntSet<*>

        if (!bitmask.contentEquals(other.bitmask)) return false
        if (values.contentEquals(other.values)) return false
        if (closure != other.closure) return false

        return true
    }

    class Node<E>(var prev: Node<E>?, var next: Node<E>?, var data: E)
}