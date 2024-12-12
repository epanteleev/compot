package ir.global

import ir.types.*


class ExternValue internal constructor(val name: String, val type: NonTrivialType): AnyGlobalValue {
    override fun name(): String = name

    override fun dump(): String {
        return "@$name = extern global $type"
    }

    override fun type(): PtrType = PtrType
    override fun toString(): String  = "@$name"
    override fun hashCode(): Int     = name.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExternValue

        return name == other.name
    }
}