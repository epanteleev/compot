package ir.types

interface NonTrivialType: Type {
    fun sizeof(): Int
}