package asm.x64

sealed interface Operand {
    fun toString(size: Int): String
}

sealed interface Register : Operand {
    fun encoding(): Int
}