package collections


class IdentityHashMap<K, V> internal constructor() : MutableMap<K, V> {
    private val map = hashMapOf<Identity<K>, V>()

    override val size: Int
        get() = map.size

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            val set = hashSetOf<IdentityEntry<K,V>>()
            for ((k, v) in map) {
                set.add(IdentityEntry(k.delegate, v))
            }
            @Suppress("UNCHECKED_CAST")
            return set as MutableSet<MutableMap.MutableEntry<K, V>>
        }

    override val keys: MutableSet<K>
        get() {
            val set = hashSetOf<K>()
            for ((k, _) in map.iterator()) {
                set.add(k.delegate)
            }
            return set
        }

    override val values: MutableCollection<V>
        get() = map.values

    override fun clear() {
        map.clear()
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun remove(key: K): V? {
        return map.remove(Identity(key))
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) {
            put(k, v)
        }
    }

    override fun put(key: K, value: V): V? {
        return map.put(Identity(key), value)
    }

    override fun get(key: K): V? {
        return map[Identity(key)]
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override fun containsKey(key: K): Boolean {
        return map.containsKey(Identity(key))
    }

    private data class Identity<T>(val delegate: T) {
        override fun hashCode(): Int {
            return delegate.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Identity<*>

            return delegate === other.delegate
        }
    }

    private data class IdentityEntry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val ret = value
            value = newValue
            return ret
        }
    }
}

fun <K, V> identityHashMapOf(): IdentityHashMap<K ,V> {
    return IdentityHashMap()
}