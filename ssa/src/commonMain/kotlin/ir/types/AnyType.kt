package ir.types

class AnyType : TrivialType, NonTrivialType, PrimitiveType {
    override fun size(): Int {
        TODO("Not yet implemented")
    }

    override fun toString(): String = "undef"

    override fun hashCode(): Int {
        return this::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }
}