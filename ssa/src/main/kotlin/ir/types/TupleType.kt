package ir.types

class TupleType(val tuple: Array<NonTrivialType>): TrivialType {
    override fun toString(): String {
        return "|${tuple.joinToString()}|"
    }

    fun innerType(index: Int): NonTrivialType {
        return tuple[index]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TupleType

        return tuple.contentEquals(other.tuple)
    }

    override fun hashCode(): Int {
        return tuple.contentHashCode()
    }
}