package collections


class IntMap<V>(val array: Array<V>) : Map<Int, V> {

    override val entries: Set<Map.Entry<Int, V>> = run() {
        val set = hashSetOf<IntMapEntry<Int, V>>()
        for ((idx, v) in array.withIndex()) {
            set.add(IntMapEntry(idx, v))
        }
        set
    }

    override val keys: Set<Int> = run() {
        val set = hashSetOf<Int>()
        for (i in array.indices) {
            set.add(i)
        }

        set
    }

    override val size: Int = array.size

    override val values: Collection<V> = array.toList()

    override fun isEmpty(): Boolean = false

    override fun get(key: Int): V? {
        if (key >= array.size || key < 0) {
            return null
        }
        return array[key]
    }

    override fun containsValue(value: V): Boolean {
        return array.contains(value)
    }

    override fun containsKey(key: Int): Boolean {
        return 0 <= key && key < array.size
    }

    private data class IntMapEntry<K, V>(override val key: K, override var value: V) : Map.Entry<K, V>
}

inline fun <reified T> intMapOf(values: Collection<T>, closure: (T) -> Int): Map<Int, T> {
    val array = arrayOfNulls<T>(values.size)
    for (elem in values) {
        array[closure(elem)] = elem
    }

    @Suppress("UNCHECKED_CAST")
    return IntMap(array as Array<T>)
}