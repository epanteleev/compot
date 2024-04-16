package ir.platform.x64.regalloc

import asm.x64.*
import ir.types.*
import asm.x64.GPRegister.*
import ir.platform.x64.CallConvention


class GPRegistersList(argumentValue: List<GPRegister>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue).toMutableList()
    private val usedCalleeSaveRegisters = mutableSetOf(rbp)

    fun pickRegister(): GPRegister? {
        if (freeRegisters.isEmpty()) {
            return null
        }
        val reg = freeRegisters.removeLast()
        if (CallConvention.gpCalleeSaveRegs.contains(reg)) {
            usedCalleeSaveRegisters.add(reg)
        }

        return reg
    }

    fun returnRegister(reg: GPRegister) {
        if (CallConvention.gpArgumentRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}