package ir.global

import ir.value.constant.*


sealed class GlobalConstant(protected open val name: String): GlobalSymbol {
    final override fun toString(): String = "@$name"
    final override fun name(): String = name

    final override fun hashCode(): Int {
        return name.hashCode()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GlobalConstant

        return name == other.name
    }

    abstract fun constant(): NonTrivialConstant
}