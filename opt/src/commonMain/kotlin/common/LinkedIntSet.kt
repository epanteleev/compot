package common


class LinkedIntSet<E>(val fwdClosure: (E) -> Int): Set<E> {
    private val values = arrayListOf<Node<E>?>()
    private var pos = 0
    private var head: Node<E>? = null
    private var tail: Node<E>? = null

    override val size: Int
        get() = pos

    fun clear() {
        values.clear()
    }

    fun addAll(elements: Collection<E>): Boolean {
        var changed = false
        for (elem in elements) {
            changed = add(elem) || changed
        }

        return changed
    }

    fun add(element: E): Boolean {
        construct { element }
        return true
    }

    fun construct(fn: (Int) -> E): E {
        val idx = pos
        pos++
        val elem = fn(idx)
        val node = Node(tail, null, elem)
        values.add(node)
        link(tail, node)
        tail = node
        return elem
    }

    private fun link(previous: Node<E>?, next: Node<E>) {
        previous?.next = next
        next.prev = previous
    }

    override fun isEmpty(): Boolean = values.all { it == null }

    override fun containsAll(elements: Collection<E>): Boolean {
        for (elem in elements) {
            if (values[fwdClosure(elem)] == null) {
                return false
            }
        }

        return true
    }

    override fun contains(element: E): Boolean {
        val idx = fwdClosure(element)
        return values[idx]?.data == element
    }

    override fun hashCode(): Int {
        return values.hashCode()
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

        if (values != other.values) return false
        if (fwdClosure != other.fwdClosure) return false

        return true
    }

    class Node<E>(var prev: Node<E>?, var next: Node<E>?, var data: E)
}

inline fun <reified E> linkedIntSetOf(values: Collection<E>, noinline closure: (E) -> Int): LinkedIntSet<E> {
    val set = LinkedIntSet(closure)
    for (v in values) {
        set.add(v)
    }

    return set
}