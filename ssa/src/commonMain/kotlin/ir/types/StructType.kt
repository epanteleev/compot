package ir.types


//TODO internal constructor
data class StructType(val name: String, val fields: List<NonTrivialType>): AggregateType {
    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = withAlignment(fields[i].size(), current)
        }
        return current
    }

    override fun size(): Int {
        var current = 0
        for (f in fields) {
            current = withAlignment(f.size(), current)
        }
        return current
    }

    override fun toString(): String = "$$name"

    fun dump(): String {
        return fields.joinToString(prefix = "$$name = type {", separator = ", ", postfix = "}")
    }

    companion object {
        private fun withAlignment(alignment: Int, value: Int): Int {
            return ((value + (alignment * 2 - 1)) / alignment) * alignment
        }
    }
}