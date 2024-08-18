package ir.platform.x64.regalloc

import asm.x64.*
import ir.platform.x64.CallConvention


internal class GPRegistersList(argumentValue: List<GPRegister>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue).toMutableList()

    fun pickRegister(excludeIf: (Register) -> Boolean): GPRegister? {
        return freeRegisters.lastOrNull { !excludeIf(it) }?.also { freeRegisters.remove(it) }
    }

    fun returnRegister(reg: GPRegister) {
        if (freeRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}