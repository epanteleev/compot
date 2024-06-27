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

    fun indexOf(element: T): Int {
        var current: LListNode? = head
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

    fun add(index: Int, value: T) {
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
            val oldHead = head
            head = value
            head!!.next = oldHead
            oldHead?.prev = head
            size++
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

    fun removeIf(predicate: (T) -> Boolean): Boolean {
        var current: LListNode? = head
        while (current != null) {
            @Suppress("UNCHECKED_CAST")
            if (predicate(current as T)) {
                val next = current.next
                remove(current)
                current = next
                continue
            }
            current = current.next
        }
        return false
    }

    fun removeLast(): T {
        val result = tail ?: throw NoSuchElementException()
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
            tail!!.next = value
            value.prev = tail
            tail = value
            value.next = null
        }
        size++
    }

    fun addAll(list: LeakedLinkedList<T>) {
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

    fun addAll(index: Int, list: LeakedLinkedList<T>) {
        checkInvariants(list)
        modificationCount++
        if (index == size) {
            addAll(list)
            return
        }
        var current: LListNode? = head
        for (i in 0 until index) {
            current = current!!.next
        }
        if (current!!.prev != null) {
            current.prev!!.next = list.first()
            list.first().prev = current.prev
        } else {
            head = list.first()
        }
        current.prev = list.last()
        list.last().next = current
        size += list.size
    }

    fun remove(node: T): T {
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

    fun transform(action: (T) -> T) {
        var current: LListNode? = head
        while (current != null) {
            @Suppress("UNCHECKED_CAST")
            current = action(current as T)
            current = current.next
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


    fun lastIndexOf(element: T): Int {
        var current = tail
        var index = size - 1
        while (current != null) {
            if (current == element) {
                return index
            }
            @Suppress("UNCHECKED_CAST")
            current = current.prev as T?
            index--
        }
        return -1
    }

    override fun toString(): String {
        return buildString {
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