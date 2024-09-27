package ir.types

import ir.Definitions.QWORD_SIZE


class StructType internal constructor(val name: String, val fields: List<NonTrivialType>): AggregateType {
    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = alignTo(current, fields[i].sizeOf()) + fields[i].sizeOf()
        }
        return alignTo(current, fields[index].sizeOf())
    }

    override fun field(index: Int): NonTrivialType {
        return fields[index]
    }

    override fun fields(): List<NonTrivialType> = fields

    override fun sizeOf(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        var offset = 0
        var alignment = 1
        for (field in fields) {
            alignment = if (field.sizeOf() <= QWORD_SIZE) {
                maxOf(alignment, field.sizeOf())
            } else {
                QWORD_SIZE
            }
            offset = alignTo(offset, alignment) + field.sizeOf()
        }
        return alignTo(offset, alignment)
    }

    override fun toString(): String = "$$name"

    fun dump(): String {
        return fields.joinToString(prefix = "$$name = type {", separator = ", ", postfix = "}")
    }

    companion object {
        private fun alignTo(value: Int, alignment: Int): Int {
            return ((value + alignment - 1) / alignment) * alignment
        }
    }
}