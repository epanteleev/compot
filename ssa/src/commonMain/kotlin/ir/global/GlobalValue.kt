package ir.global

import ir.types.*


class GlobalValue(val name: String, val data: GlobalConstant): GlobalSymbol {
    override fun name(): String {
        return name
    }

    override fun dump(): String {
        return "@$name = global ${data.contentType()} @${data.name()}"
    }

    override fun type(): NonTrivialType = Type.Ptr

    fun dataType(): Type = data.type()

    override fun toString(): String {
        return "@$name"
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GlobalValue

        return name == other.name
    }

    fun content(): String = data()

    fun data(): String {
        return data.data()
    }

    fun contentType(): Type = data.contentType()
}