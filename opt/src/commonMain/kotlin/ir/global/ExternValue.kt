package ir.global

import ir.types.Type
import ir.types.PointerType
import ir.types.NonTrivialType


class ExternValue(val name: String, val type: NonTrivialType): AnyGlobalValue {
    override fun name(): String = name

    override fun dump(): String {
        return "@$name = extern global $type"
    }

    override fun type(): PointerType = Type.Ptr

    override fun toString(): String {
        return "@$name"
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExternValue

        return name == other.name
    }
}