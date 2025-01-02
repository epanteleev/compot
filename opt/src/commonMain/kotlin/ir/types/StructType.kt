package ir.types

import ir.Definitions
import ir.Definitions.BYTE_SIZE


class StructType internal constructor(val name: String, val fields: List<NonTrivialType>): AggregateType {
    private val alignments = alignments()
    private val maxAlignment by lazy { alignments.maxOrNull() ?: 1 }

    override fun alignmentOf(): Int = maxAlignment

    private fun alignments(): IntArray {
        var current = 0
        val result = IntArray(fields.size)
        for (i in fields.indices) {
            val field = fields[i]
            val alignment = field.alignmentOf()
            current = Definitions.alignTo(current + field.sizeOf(), alignment)
            result[i] = alignment
        }
        return result
    }

    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = Definitions.alignTo(current + fields[i].sizeOf(), alignments[i])
        }
        return Definitions.alignTo(current, alignments[index])
    }

    override fun field(index: Int): NonTrivialType {
        return fields[index]
    }

    override fun fields(): List<NonTrivialType> = fields

    override fun sizeOf(): Int {
        if (fields.isEmpty()) {
            return BYTE_SIZE
        }
        var offset = 0
        for (idx in fields.indices) {
            offset = Definitions.alignTo(offset + fields[idx].sizeOf(), alignments[idx])
        }
        return Definitions.alignTo(offset, alignmentOf())
    }

    override fun toString(): String = "$$name"

    fun dump(): String {
        return fields.joinToString(prefix = "$$name = type {", separator = ", ", postfix = "}")
    }
}