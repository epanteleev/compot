package collections


class IntMap<K, V>(private val valuesArray: Array<V?>, private val keysArray: Array<K?>, val closure: (K) -> Int) : MutableMap<K, V> {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            val keyToValue = hashSetOf<IntMapEntry<K, V>>()
            keys.forEachWith(values) { k, v ->
                if (k == null || v == null) {
                    return@forEachWith
                }

                keyToValue.add(IntMapEntry(k, v))
            }

            @Suppress("UNCHECKED_CAST")
            return keyToValue as MutableSet<MutableMap.MutableEntry<K, V>>
        }

    override val keys: MutableSet<K>
        get() = keysArray.fold(hashSetOf()) { acc, v ->
            if (v != null) {
                acc.add(v)
            }
            acc
        }

    override val size: Int
        get() = valuesArray.size

    override val values: MutableCollection<V>
        get() = valuesArray.fold(arrayListOf()) { acc, v ->
            if (v != null) {
                acc.add(v)
            }
            acc
        }

    override fun clear() {
        valuesArray.fill(null)
        keysArray.fill(null)
    }

    override fun isEmpty(): Boolean = valuesArray.find { it != null } != null

    override fun remove(key: K): V? {
        val idx = closure(key)
        if (idx >= valuesArray.size || idx < 0) {
            return null
        }
        val item = valuesArray[idx]
        valuesArray[idx] = null
        keysArray[idx] = null
        return item
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) {
            put(k, v)
        }
    }

    override fun put(key: K, value: V): V? {
        val idx = closure(key)
        if (idx >= valuesArray.size || idx < 0) {
            return null
        }
        val item = valuesArray[idx]
        valuesArray[idx] = value
        keysArray[idx] = key
        return item
    }

    override fun get(key: K): V? {
        val idx = closure(key)
        if (idx >= valuesArray.size || idx < 0) {
            return null
        }

        val ret = valuesArray[idx]
        assert(ret == null || keysArray[idx] == key) {
            "expect, but idx=$idx, key=$key"
        }
        return ret
    }

    override fun containsValue(value: V): Boolean {
        return valuesArray.contains(value)
    }

    override fun containsKey(key: K): Boolean {
        val idx = closure(key)
        assert(0 <= idx && keysArray[idx] == key) { "expect, but idx=$idx, key=$key" }
        return idx < valuesArray.size
    }

    private data class IntMapEntry<K, V>(override val key: K, override var value: V?) : Map.Entry<K, V?>
}

inline fun <reified K, reified T> intMapOf(values: Collection<K>, noinline closure: (K) -> Int): MutableMap<K, T> {
    return IntMap(arrayOfNulls<T>(values.size), values.toTypedArray(), closure)
}

inline fun <reified K, reified T> intMapOf(size: Int, noinline closure: (K) -> Int): MutableMap<K, T> {
    return IntMap(arrayOfNulls<T>(size), arrayOfNulls<K>(size), closure)
}