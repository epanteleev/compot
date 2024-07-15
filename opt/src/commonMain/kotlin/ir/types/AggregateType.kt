package ir.types

sealed interface AggregateType : NonTrivialType {
    fun offset(index: Int): Int
    fun field(index: Int): NonTrivialType
    fun fields(): List<NonTrivialType>
}