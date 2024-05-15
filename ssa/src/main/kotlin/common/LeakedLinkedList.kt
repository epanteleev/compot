package common


// Ordinary linked list with leaked abstraction
class LeakedLinkedList<T> : List<LListNode<T>> {
    private var head: LListNode<T>? = null
    private var tail: LListNode<T>? = null
    override var size = 0

    override fun get(index: Int): LListNode<T> {
        var current = head
        for (i in 0 until index) {
            current = current!!.next
        }
        return current!!
    }

    override fun indexOf(element: LListNode<T>): Int {
        var current = head
        var index = 0
        while (current != null) {
            if (current == element) {
                return index
            }
            current = current.next
            index++
        }
        return -1
    }

    override fun containsAll(elements: Collection<LListNode<T>>): Boolean {
        for (element in elements) {
            if (!contains(element)) {
                return false
            }
        }
        return true
    }

    override fun contains(element: LListNode<T>): Boolean {
        var current = head
        while (current != null) {
            if (current == element) {
                return true
            }
            current = current.next
        }
        return false
    }

    fun add(value: T): LListNode<T> {
        val node = LListNode(value)
        if (head == null) {
            head = node
            tail = node
        } else {
            tail!!.next = node
            node.prev = tail
            tail = node
        }
        size++
        return node
    }

    fun remove(node: LListNode<T>) {
        if (node.prev != null) {
            node.prev!!.next = node.next
        } else {
            head = node.next
        }
        if (node.next != null) {
            node.next!!.prev = node.prev
        } else {
            tail = node.prev
        }
        size--
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): Iterator<LListNode<T>> {
        return object : Iterator<LListNode<T>> {
            private var current = head
            override fun hasNext(): Boolean = current != null
            override fun next(): LListNode<T> {
                val result = current
                current = current!!.next
                return result!!
            }
        }
    }

    override fun listIterator(): ListIterator<LListNode<T>> {
        return object : ListIterator<LListNode<T>> {
            private var current = head
            private var index = 0
            override fun hasNext(): Boolean = current != null
            override fun hasPrevious(): Boolean = current != null
            override fun next(): LListNode<T> {
                val result = current
                current = current!!.next
                index++
                return result!!
            }
            override fun nextIndex(): Int = index
            override fun previous(): LListNode<T> {
                val result = current
                current = current!!.prev
                index--
                return result!!
            }
            override fun previousIndex(): Int = index - 1
        }
    }

    override fun listIterator(index: Int): ListIterator<LListNode<T>> {
        return object : ListIterator<LListNode<T>> {
            private var current = head
            private var currentIndex = 0
            init {
                for (i in 0 until index) {
                    current = current!!.next
                    currentIndex++
                }
            }
            override fun hasNext(): Boolean = current != null
            override fun hasPrevious(): Boolean = current != null
            override fun next(): LListNode<T> {
                val result = current
                current = current!!.next
                currentIndex++
                return result!!
            }
            override fun nextIndex(): Int = currentIndex
            override fun previous(): LListNode<T> {
                val result = current
                current = current!!.prev
                currentIndex--
                return result!!
            }
            override fun previousIndex(): Int = currentIndex - 1
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<LListNode<T>> {
        val result = mutableListOf<LListNode<T>>()
        var current = head
        for (i in 0 until fromIndex) {
            current = current!!.next
        }
        for (i in fromIndex until toIndex) {
            result.add(current!!)
            current = current.next
        }
        return result
    }

    override fun lastIndexOf(element: LListNode<T>): Int {
        var current = tail
        var index = size - 1
        while (current != null) {
            if (current == element) {
                return index
            }
            current = current.prev
            index--
        }
        return -1
    }

    override fun toString(): String {
        return buildString {
            append("[")
            var current = head
            while (current != null) {
                append(current.value)
                current = current.next
                if (current != null) {
                    append(", ")
                }
            }
            append("]")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LeakedLinkedList<*>

        if (size != other.size) return false
        var current = head
        var otherCurrent = other.head
        while (current != null) {
            if (current.value != otherCurrent!!.value) {
                return false
            }
            current = current.next
            otherCurrent = otherCurrent.next
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        var current = head
        while (current != null) {
            result = 31 * result + current.value.hashCode()
            current = current.next
        }
        return result
    }

    fun first(): LListNode<T> = head!!

    fun firstOrNull(): T? = head?.value

    fun last(): LListNode<T> = tail!!

    fun lastOrNull(): T? = tail?.value

    fun clear() {
        head = null
        tail = null
        size = 0
    }
}

class LListNode<T>(val value: T) {
    internal var next: LListNode<T>? = null
    internal var prev: LListNode<T>? = null

    fun next(): T? = next?.value
    fun prev(): T? = prev?.value
}

inline fun<reified T> leakedLinkedListOf(values: Collection<T>): LeakedLinkedList<T> {
    val list = LeakedLinkedList<T>()
    for (v in values) {
        list.add(v)
    }
    return list
}

inline fun<reified T> leakedLinkedListOf(vararg values: T): LeakedLinkedList<T> {
    return leakedLinkedListOf(values.asList())
}