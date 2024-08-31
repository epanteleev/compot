package asm.x64

class NameAssistant {
    private var constantCounter = 0

    fun nextConstant(): String = ".LC${constantCounter++}"
}