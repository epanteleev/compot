package ir.global

import ir.types.*
import ir.value.constant.*
import ir.value.UsableValue
import ir.instruction.Instruction
import ir.attributes.GlobalValueAttribute


class GlobalValue private constructor(val name: String, private val init: NonTrivialConstant, private val attribute: GlobalValueAttribute): AnyGlobalValue, UsableValue {
    override var usedIn: MutableList<Instruction> = mutableListOf()

    fun initializer(): NonTrivialConstant = init

    override fun name(): String = name

    override fun dump(): String {
        return "@$name = global ${contentType()} $init !$attribute"
    }

    fun contentType(): NonTrivialType = init.type() as NonTrivialType

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

    fun attribute(): GlobalValueAttribute = attribute

    companion object {
        fun create(name: String, initializer: NonTrivialConstant, attributes: GlobalValueAttribute): GlobalValue {
            return GlobalValue(name, initializer, attributes)
        }
    }
}