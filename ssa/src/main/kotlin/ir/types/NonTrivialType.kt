package ir.types

interface NonTrivialType: Type {
    fun size(): Int
}