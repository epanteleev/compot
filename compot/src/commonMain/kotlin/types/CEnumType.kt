package types

data class CEnumType(val name: String, private val enumerators: Map<String, Int>): CPrimitive() {
    override fun toString(): String = name

    override fun size(): Int = INT.size()

    fun hasEnumerator(name: String): Boolean {
        return enumerators.contains(name)
    }

    fun enumerator(name: String): Int? {
        return enumerators[name] //TODO temporal
    }
}