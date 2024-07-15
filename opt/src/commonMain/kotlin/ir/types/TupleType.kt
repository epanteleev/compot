package ir.types


class TupleType(val tuple: Array<NonTrivialType>): TrivialType {
    override fun toString(): String {
        return "|${tuple.joinToString()}|"
    }

    fun innerType(index: Int): NonTrivialType {
        return tuple[index]
    }

    inline fun<reified T: NonTrivialType> asInnerType(index: Int): T {
        val type = innerType(index)
        if (type !is T) {
            throw RuntimeException("unexpected type: '$type'")
        }

        return type
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TupleType

        return tuple.contentEquals(other.tuple)
    }

    override fun hashCode(): Int {
        return tuple.contentHashCode()
    }
}