package ir.attributes

import ir.types.StructType


class ByValue(val argumentIndex: Int, val aggregateType: StructType): FunctionAttribute, ArgumentValueAttribute {
    override fun toString(): String = "!byval[$argumentIndex]"

    override fun hashCode(): Int {
        return argumentIndex xor this::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByValue) return false
        return argumentIndex == other.argumentIndex && aggregateType == other.aggregateType
    }
}