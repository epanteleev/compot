package ir.types


data class StructType internal constructor(val name: String, val fields: List<NonTrivialType>): AggregateType {
    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = alignTo(current, fields[i].size()) + fields[i].size()
        }
        return alignTo(current, fields[index].size())
    }

    override fun field(index: Int): NonTrivialType {
        return fields[index]
    }

    override fun size(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        var offset = 0
        var alignment = 1
        for (field in fields) {
            alignment = maxOf(alignment, field.size())
            offset = alignTo(offset, alignment) + field.size()
        }
        return alignTo(offset, 8)
    }

    override fun toString(): String = "$$name"

    fun dump(): String {
        return fields.joinToString(prefix = "$$name = type {", separator = ", ", postfix = "}")
    }

    companion object {
        private fun alignTo(value: Int, alignment: Int): Int {
            return ((value + alignment - 1) / alignment) * alignment
        }

        private fun withAlignment(alignment: Int, value: Int): Int {
            return ((value + (alignment * 2 - 1)) / alignment) * alignment
        }
    }
}