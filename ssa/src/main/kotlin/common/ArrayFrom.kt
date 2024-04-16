package common

inline fun<reified T> arrayFrom(values: Collection<T>): Array<T> {
    val array = arrayOfNulls<T>(values.size)
    for ((idx, v) in values.withIndex()) {
        array[idx] = v
    }

    @Suppress("UNCHECKED_CAST")
    return array as Array<T>
}