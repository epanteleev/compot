package ir.global

import ir.attributes.GlobalValueAttribute
import ir.types.*


class GlobalValue(val name: String, val data: GlobalConstant, val attribute: List<GlobalValueAttribute>): GlobalSymbol {
    init {
        require(data.type() is PrimitiveType) {
            "GlobalValue data must be a PrimitiveType, but was '${data.type()}'"
        }
    }

    override fun name(): String = name

    override fun dump(): String {
        return buildString {
            append("@$name = global ${data.contentType()} ${data.data()}")
            for (attr in attribute) {
                append(" !$attr")
            }
        }
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

        other as GlobalValue

        return name == other.name
    }

    fun data(): String {
        return data.data()
    }
}