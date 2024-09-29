package ir.types

object UndefType : TrivialType, NonTrivialType, PrimitiveType {
    override fun sizeOf(): Int {
        return 0
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