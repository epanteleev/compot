package ir.platform.x64.regalloc

import asm.x64.*
import ir.types.*
import asm.x64.GPRegister.*
import ir.platform.x64.CallConvention


class GPRegistersList(argumentValue: List<GPRegister>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue).toMutableList()
    private val usedCalleeSaveRegisters = mutableSetOf(rbp)

    fun pickRegister(type: PrimitiveType): GPRegister? {
        require(type !is FloatingPointType ) { "found $type" }

        if (freeRegisters.isEmpty()) {
            return null
        }
        val reg = freeRegisters.removeLast()
        if (reg.isCallEESave) {
            usedCalleeSaveRegisters.add(reg)
        }

        return reg
    }

    fun returnRegister(reg: GPRegister) {
        if (reg.isArgument) {
            return
        }

        freeRegisters.add(reg)
    }
}