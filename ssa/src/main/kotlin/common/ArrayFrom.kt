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