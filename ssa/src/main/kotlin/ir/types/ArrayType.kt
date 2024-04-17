package ir.types


data class ArrayType(private val type: NonTrivialType, val size: Int) : AggregateType {
    fun elementType(): NonTrivialType = type

    override fun size(): Int {
        return size * type.size()
    }

    override fun offset(index: Int): Int {
        return index * type.size()
    }

    override fun toString(): String {
        return "<$type x $size>"
    }
}