package ir.value

import ir.attributes.ArgumentValueAttribute
import ir.types.*
import ir.instruction.Instruction


class ArgumentValue(private val index: Int, private val tp: NonTrivialType, val attributes: Set<ArgumentValueAttribute>): LocalValue {
    override var usedIn: MutableList<Instruction> = arrayListOf()
    override fun name(): String {
        return "arg$index"
    }

    override fun type(): NonTrivialType = when (tp) {
        is AggregateType -> Type.Ptr //TODO might be simplified!!???!?!
        is PrimitiveType -> tp
    }

    fun contentType(): NonTrivialType = tp

    override fun hashCode(): Int = index

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArgumentValue) return false
        return index == other.index
    }

    fun position(): Int {
        return index
    }

    override fun toString(): String = "%${name()}"
}