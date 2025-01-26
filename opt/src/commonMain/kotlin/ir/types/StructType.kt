package ir.types

import ir.Definitions
import ir.Definitions.BYTE_SIZE


class StructType private constructor(
    val name: String,
    val fields: List<NonTrivialType>,
    private val alignments: IntArray,
    private val maxAlignment: Int
) : AggregateType {
    override fun alignmentOf(): Int = maxAlignment

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

    companion object {
        private fun alignments(fields: List<NonTrivialType>): IntArray {
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

        fun create(name: String, fields: List<NonTrivialType>): StructType {
            val alignments = alignments(fields)
            val maxAlignment = alignments.maxOrNull() ?: BYTE_SIZE
            return StructType(name, fields, alignments, maxAlignment)
        }

        fun create(name: String, fields: List<NonTrivialType>, alignOf: Int): StructType {
            val alignments = alignments(fields)
            return StructType(name, fields, alignments, alignOf)
        }
    }
}