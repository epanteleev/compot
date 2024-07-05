package ir.types

interface AggregateType : NonTrivialType {
    fun offset(index: Int): Int
    fun field(index: Int): NonTrivialType
}