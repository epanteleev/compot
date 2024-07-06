package ir.types


data class ArrayType(private val type: NonTrivialType, val size: Int) : AggregateType {
    fun elementType(): NonTrivialType = type

    override fun sizeOf(): Int {
        return size * type.sizeOf()
    }

    override fun offset(index: Int): Int {
        return index * type.sizeOf()
    }

    override fun field(index: Int): NonTrivialType {
        return type
    }

    override fun toString(): String {
        return "<$type x $size>"
    }
}