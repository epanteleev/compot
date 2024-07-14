package ir.types

import ir.Definitions.POINTER_SIZE


object PointerType: PrimitiveType {
    override fun sizeOf(): Int = POINTER_SIZE

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