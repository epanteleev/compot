package ir.platform.regalloc

import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention


class XmmRegisterList(arguments: List<XmmRegister>) {
    private var freeRegisters = CallConvention.availableXmmRegisters(arguments).toMutableList()
    private val usedCalleeSaveRegisters = mutableSetOf<XmmRegister>()

    fun pickRegister(type: FloatingPointType): XmmRegister? {
        if (freeRegisters.isEmpty()) {
            return null
        }
        val reg = freeRegisters.removeLast()
        if (reg.isCallEESave) {
            usedCalleeSaveRegisters.add(reg)
        }

        return reg
    }

    fun returnRegister(reg: XmmRegister) {
        if (reg.isArgument) {
            return
        }

        freeRegisters.add(reg)
    }
}