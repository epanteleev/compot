package ir.platform.x64.regalloc

import ir.value.LocalValue


class Group(val values: List<LocalValue>) {
    fun contains(value: LocalValue): Boolean = values.contains(value)

    override fun hashCode(): Int = values.hashCode()

    operator fun iterator(): Iterator<LocalValue> = values.iterator()

    override fun toString(): String {
        return values.joinToString(prefix = "[", separator = ",", postfix = "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Group

        if (values != other.values) return false

        return true
    }
}