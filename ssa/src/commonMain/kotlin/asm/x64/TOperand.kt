package asm.x64

interface Operand {
    fun toString(size: Int): String
}

interface Register : Operand {
    fun encoding(): Int
}