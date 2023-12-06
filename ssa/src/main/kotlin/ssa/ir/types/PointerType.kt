package ir.types

class PointerType internal constructor() : PrimitiveType {

    override fun size(): Int {
        return 8
    }

    override fun toString(): String {
        return "ptr"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}