package ir.types

object BottomType : TrivialType, NonTrivialType, PrimitiveType {
    override fun sizeOf(): Int {
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