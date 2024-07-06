package ir.types

interface NonTrivialType: Type {
    fun sizeOf(): Int
}