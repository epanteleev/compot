package common

import kotlin.jvm.JvmInline

@JvmInline
value class ArrayWrapper<T>(private val array: Array<T>): List<T> {
    override val size: Int
        get() = array.size

    override fun contains(element: T): Boolean {
        return array.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (element in elements) {
            if (!array.contains(element)) {
                return false
            }
        }

        return true
    }

    override fun get(index: Int): T {
        return array[index]
    }

    override fun indexOf(element: T): Int {
        return array.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return array.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return array.iterator()
    }

    override fun lastIndexOf(element: T): Int {
        return array.lastIndexOf(element)
    }

    override fun listIterator(): ListIterator<T> {
        return ArrayWrapperIterator(array)
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return ArrayWrapperIterator(array, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return array.asList().subList(fromIndex, toIndex)
    }

    class ArrayWrapperIterator<T>(private val array: Array<T>, private var index: Int = 0): ListIterator<T> {
        override fun hasNext(): Boolean {
            return index < array.size
        }

        override fun hasPrevious(): Boolean {
            return index > 0
        }

        override fun next(): T {
            return array[index++]
        }

        override fun nextIndex(): Int {
            return index
        }

        override fun previous(): T {
            return array[--index]
        }

        override fun previousIndex(): Int {
            return index - 1
        }
    }
}

inline fun<T> arrayWrapperOf(array: Array<T>): ArrayWrapper<T> {
    return ArrayWrapper(array)
}