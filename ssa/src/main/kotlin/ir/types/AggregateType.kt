package ir.types

interface AggregateType : Type {
    fun offset(index: Int): Int
}