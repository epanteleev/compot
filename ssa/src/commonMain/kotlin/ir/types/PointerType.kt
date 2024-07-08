package ir.types


object PointerType: PrimitiveType {
    override fun sizeOf(): Int {
        return 8
    }

    override fun toString(): String {
        return "ptr"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}