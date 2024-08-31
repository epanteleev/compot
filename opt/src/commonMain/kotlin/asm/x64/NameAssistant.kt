package asm.x64

class NameAssistant {
    private var constantCounter = 0
    private var functionCounter = 0

    fun nextConstant(): String = ".loc.constant.${constantCounter++}"
    fun newLocalLabel(asm: Assembler, id: Int): String = ".L${asm.id}.$id"
    fun nextFunction(): Int = functionCounter++
}