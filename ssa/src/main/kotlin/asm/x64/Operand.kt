package asm.x64

interface AnyOperand {
    fun toString(size: Int): String
}

interface Operand : AnyOperand

interface Register : Operand {
    val isCallERSave: Boolean
    val isCallEESave: Boolean
    val isArgument: Boolean
}