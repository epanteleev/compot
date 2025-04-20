package common


// Ordinary linked list with leaked abstraction
abstract class LeakedLinkedList<T: LListNode>: Collection<T> {
    private var head: T? = null
    private var tail: T? = null
    override var size = 0
    private var modificationCount = 0

    private fun checkInvariants(newElem: T) {
        if (newElem.prev != null) {
            throw IllegalStateException("prev should be null")
        }
        if (newElem.next != null) {
            throw IllegalStateException("next should be null")
        }
    }

    private fun checkInvariants(other: LeakedLinkedList<T>) {
        if (other.head == null) {
            return
        }
        if (other.head!!.prev != null) {
            throw IllegalStateException("prev should be null")
        }
        if (other.tail!!.next != null) {
            throw IllegalStateException("next should be null")
        }
    }

    operator fun get(index: Int): T {
        var current: LListNode? = head
        for (i in 0 until index) {
            current = current!!.next
        }
        @Suppress("UNCHECKED_CAST")
        return current as T
    }

    private fun add(index: Int, value: T) {
        checkInvariants(value)
        modificationCount++
        if (index == size) {
            add(value)
            return
        }
        var current: LListNode? = head
        for (i in 0 until index) {
            current = current!!.next
        }
        value.next = current
        value.prev = current!!.prev
        current.prev = value
        if (value.prev != null) {
            value.prev!!.next = value
        } else {
            head = value
        }
        size++
    }

    // if node is null, add to the beginning
    fun addBefore(node: T?, value: T) {
        checkInvariants(value)
        modificationCount++
        if (node == null) {
            add(0, value)
            return
        }
        value.prev = node.prev
        value.next = node
        node.prev = value
        if (value.prev != null) {
            value.prev!!.next = value
        } else {
            head = value
        }
        size++
    }

    // if node is null, add to the end
    fun addAfter(node: T?, value: T) {
        checkInvariants(value)
        modificationCount++
        if (node == null) {
            add(value)
            return
        }
        value.next = node.next
        value.prev = node
        node.next = value
        if (value.next != null) {
            value.next!!.prev = value
        } else {
            tail = value
        }
        size++
    }

    fun removeLast(): T {
        val result = tail ?: throw NoSuchElementException()
        remove(result)
        return result
    }

    fun removeFirst(): T {
        val result = head ?: throw NoSuchElementException()
        remove(result)
        return result
    }

    fun removeAt(index: Int): T {
        var current: LListNode? = head
        for (i in 0 until index) {
            current = current!!.next
        }
        @Suppress("UNCHECKED_CAST")
        remove(current as T)
        return current
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (element in elements) {
            if (!contains(element)) {
                return false
            }
        }
        return true
    }

    override fun contains(element: T): Boolean {
        var current: LListNode? = head
        while (current != null) {
            if (current == element) {
                return true
            }
            current = current.next
        }
        return false
    }

    fun add(value: T) {
        checkInvariants(value)
        modificationCount++
        if (head == null) {
            head = value
            tail = value
        } else {
            assertion(tail != null) {
                "tail should not be null"
            }
            tail!!.next = value
            value.prev = tail
            tail = value
            value.next = null
        }
        size++
    }

    fun addAll(list: LeakedLinkedList<T>) {
        if (list.isEmpty()) {
            return
        }
        checkInvariants(list)
        modificationCount++
        if (head == null) {
            head = list.first()
            tail = list.last()
        } else {
            tail!!.next = list.first()
            list.first().prev = tail
            tail = list.last()
        }
        size += list.size
    }

    fun addAll(before: LListNode, list: LeakedLinkedList<T>) {
        if (list.isEmpty()) {
            return
        }
        checkInvariants(list)
        modificationCount++
        if (before.prev != null) {
            before.prev!!.next = list.first()
            list.first().prev = before.prev
        } else {
            head = list.first()
        }
        before.prev = list.last()
        list.last().next = before
        size += list.size
    }

    fun remove(node: T): T {
        if (node.prev == null && node.next == null) {
            throw IllegalStateException("node is not in the list")
        }

        modificationCount++

        if (node.prev != null) {
            node.prev!!.next = node.next
        } else {
            @Suppress("UNCHECKED_CAST")
            head = node.next as T?
        }

        if (node.next != null) {
            node.next!!.prev = node.prev
        } else {
            @Suppress("UNCHECKED_CAST")
            tail = node.prev as T?
        }

        size--
        node.next = null
        node.prev = null

        return node
    }

    fun forEach(action: (T) -> Unit) {
        var current: LListNode? = head
        val expectedModificationCount = modificationCount
        while (current != null) {
            @Suppress("UNCHECKED_CAST")
            action(current as T)
            if (expectedModificationCount != modificationCount) {
                throw ConcurrentModificationException()
            }
            current = current.next
        }
    }

    fun transform(action: (T) -> T?) {
        var current: LListNode? = head
        while (current != null) {
            @Suppress("UNCHECKED_CAST")
            current = action(current as T)
            current = if (current != null) {
                current.next
            } else {
                head // TODo
            }
        }
    }

    override fun isEmpty(): Boolean = size == 0
    fun isNotEmpty(): Boolean = size != 0

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var current = head
            private val expectedModificationCount = modificationCount
            override fun hasNext(): Boolean = current != null
            override fun next(): T {
                val result = current
                if (expectedModificationCount != modificationCount) {
                    throw ConcurrentModificationException()
                }
                @Suppress("UNCHECKED_CAST")
                current = current!!.next as T?
                return result!!
            }
        }
    }

    override fun toString(): String = buildString {
        append("[")
        var current = head
        while (current != null) {
            append(current.toString())
            @Suppress("UNCHECKED_CAST")
            current = current.next as T?
            if (current != null) {
                append(", ")
            }
        }
        append("]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LeakedLinkedList<*>

        if (size != other.size) return false
        var current = head
        var otherCurrent = other.head
        while (current != null) {
            if (current != otherCurrent) {
                return false
            }
            @Suppress("UNCHECKED_CAST")
            current = current.next as T?
            @Suppress("UNCHECKED_CAST")
            otherCurrent = otherCurrent.next as T?
        }
        return true
    }

    override fun hashCode(): Int {
        var result = head?.hashCode() ?: 0
        result = 31 * result + (tail?.hashCode() ?: 0)
        result = 31 * result + size
        return result
    }

    fun first(): T = head!!

    fun firstOrNull(): T? = head

    fun last(): T = tail!!

    fun lastOrNull(): T? = tail

    fun clear() {
        head = null
        tail = null
        size = 0
    }
}

abstract class LListNode {
    internal var next: LListNode? = null
    internal var prev: LListNode? = null

    open fun prev(): LListNode? {
        return prev
    }
    open fun next(): LListNode? {
        return next
    }
}