package common

inline fun<reified T> arrayFrom(values: Collection<T>): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return arrayNullsFrom(values) as Array<T>
}

inline fun<reified T> arrayNullsFrom(values: Collection<T>): Array<T?> {
    val array = arrayOfNulls<T>(values.size)
    for ((idx, v) in values.withIndex()) {
        array[idx] = v
    }

    return array
}

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