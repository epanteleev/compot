package ir.types

sealed interface NonTrivialType: Type {
    fun sizeOf(): Int
}