package ir.value

import ir.types.*
import ir.instruction.Instruction
import ir.attributes.ArgumentValueAttribute


class ArgumentValue(private val index: Int, private val tp: NonTrivialType, val attributes: Set<ArgumentValueAttribute>): LocalValue {
    override var usedIn: MutableList<Instruction> = arrayListOf()
    override fun name(): String = "arg$index"

    override fun type(): NonTrivialType = tp

    override fun hashCode(): Int = index

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ArgumentValue

        return index == other.index
    }

    fun position(): Int = index

    override fun toString(): String = "%${name()}"
}