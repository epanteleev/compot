package ir.types


data class ArrayType(private val type: NonTrivialType, val length: Int) : AggregateType {
    private var maxAlignment = Int.MIN_VALUE
    init {
        require(length >= 0) { "Array size must be greater than 0, but: size=$length" }
    }

    override fun alignmentOf(): Int {
        if (maxAlignment == Int.MIN_VALUE) {
            maxAlignment = type.alignmentOf()
        }

        return maxAlignment
    }

    fun elementType(): NonTrivialType = type

    override fun sizeOf(): Int {
        return length * type.sizeOf()
    }

    override fun offset(index: Int): Int {
        return index * type.sizeOf()
    }

    override fun field(index: Int): NonTrivialType {
        return type
    }

    override fun fields(): List<NonTrivialType> {
        return generateSequence { type }.take(length).toList()
    }

    override fun toString(): String {
        return "<$type x $length>"
    }
}