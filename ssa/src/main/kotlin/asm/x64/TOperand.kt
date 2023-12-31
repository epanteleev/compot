package asm.x64

interface Operand {
    fun toString(size: Int): String
}

interface Register : Operand {
    val isCallERSave: Boolean
    val isCallEESave: Boolean
    val isArgument: Boolean
}