package ir.value.constant

import ir.types.*


object UndefValue: Constant, PrimitiveConstant {
    fun name(): String = toString()

    override fun type(): UndefType = UndefType

    override fun toString(): String = "undef"

    override fun hashCode(): Int {
        return UndefType::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }
}