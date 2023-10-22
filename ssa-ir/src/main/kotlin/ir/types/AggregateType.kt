package ir.types

data class AggregateType(val name: String, val fields: List<Type>) : Type {
    override fun ptr(): Type {
        return PointerType(this)
    }

    override fun size(): Int {
        return fields.fold(0) { acc, it -> acc + it.size() }
    }
}