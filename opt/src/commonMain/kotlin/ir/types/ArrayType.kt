package ir.types


data class ArrayType(private val type: NonTrivialType, val size: Int) : AggregateType {
    init {
        require(size >= 0) { "Array size must be greater than 0, but: size=$size" }
    }

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

    override fun fields(): List<NonTrivialType> {
        return generateSequence { type }.take(size).toList()
    }

    override fun toString(): String {
        return "<$type x $size>"
    }
}