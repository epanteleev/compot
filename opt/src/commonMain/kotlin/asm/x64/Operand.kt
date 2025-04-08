package asm.x64

sealed interface Operand {
    fun toString(size: Int): String
}

sealed interface VReg : Operand

sealed interface Register : VReg {
    fun encoding(): Int
}

