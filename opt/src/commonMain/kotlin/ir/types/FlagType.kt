package ir.types

import ir.Definitions.BYTE_SIZE


object FlagType : NonTrivialType {
    override fun toString(): String = "u1"
    override fun sizeOf(): Int = BYTE_SIZE

    override fun hashCode(): Int {
        return this::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }
}