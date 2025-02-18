package common

inline fun<reified T, reified U> arrayFrom(values: Collection<T>, initializer: (T) -> U): Array<U> {
    val array = arrayOfNulls<U>(values.size)
    for ((idx, v) in values.withIndex()) {
        array[idx] = initializer(v)
    }

    @Suppress("UNCHECKED_CAST")
    return array as Array<U>
}

inline fun<reified T, reified U> arrayFrom(values: Collection<T>, initializer: (Int, T) -> U): Array<U> {
    val array = arrayOfNulls<U>(values.size)
    for ((idx, v) in values.withIndex()) {
        array[idx] = initializer(idx, v)
    }

    @Suppress("UNCHECKED_CAST")
    return array as Array<U>
}

inline fun<reified T, reified U> arrayFrom(values: Array<T>, initializer: (T) -> U): Array<U> {
    val array = arrayOfNulls<U>(values.size)
    for ((idx, v) in values.withIndex()) {
        array[idx] = initializer(v)
    }

    @Suppress("UNCHECKED_CAST")
    return array as Array<U>
}

inline fun<reified T> arrayWith(size: Int, initializer: (Int) -> T): Array<T> {
    val array = arrayOfNulls<T>(size)
    for (idx in 0 until size) {
        array[idx] = initializer(idx)
    }

    @Suppress("UNCHECKED_CAST")
    return array as Array<T>
}


inline fun <reified T> Collection<T>.toTypedArray(appended: T): Array<T> {
    val result = arrayOfNulls<T>(size + 1)
    var index = 0
    for (element in this) {
        result[index++] = element
    }
    result[index] = appended
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

inline fun <T> Array<T?>.getOrSet(index: Int, defaultValue: (Int) -> T): T {
    val v = get(index)
    if (v != null) {
        return v
    }

    val newValue = defaultValue(index)
    set(index, newValue)
    return newValue
}