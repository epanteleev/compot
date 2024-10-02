package ir.types


class StructType internal constructor(val name: String, val fields: List<NonTrivialType>): AggregateType {
    private val alignments = alignments()
    private var maxAlignment = Int.MIN_VALUE
    init {
        require(fields.all { it !is FlagType }) { "StructType cannot contain FlagType" }
    }

    override fun maxAlignment(): Int {
        if (maxAlignment == Int.MIN_VALUE) {
            maxAlignment = alignments.maxOrNull() ?: 1
        }
        return maxAlignment
    }

    private fun alignments(): IntArray {
        var current = 0
        var alignment = 1
        val result = IntArray(fields.size)
        for (i in fields.indices) {
            val field = fields[i]
            alignment = align(alignment, field)
            current = alignTo(current + field.sizeOf(), alignment)
            result[i] = alignment
        }
        return result
    }

    private fun align(alignment: Int, field: NonTrivialType): Int {
        return if (field is AggregateType) {
            maxOf(alignment, field.maxAlignment())
        } else {
            maxOf(alignment, field.sizeOf())
        }
    }

    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = alignTo(current + fields[i].sizeOf(), alignments[i])
        }
        return alignTo(current, alignments[index])
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
        for (idx in fields.indices) {
            offset = alignTo(offset + fields[idx].sizeOf(), alignments[idx])
        }
        return offset
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