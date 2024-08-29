package ir.global

import ir.attributes.GlobalValueAttribute
import ir.types.*
import ir.value.Constant


class GlobalValue internal constructor(val name: String, private val type: NonTrivialType, private val init: Constant, private val attribute: GlobalValueAttribute): AnyGlobalValue {
    fun initializer(): Constant = init

    override fun name(): String = name

    override fun dump(): String {
        return "@$name = global $type ${init.data()} !$attribute"
    }

    fun contentType(): NonTrivialType = type

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
        return init.data()
    }
}