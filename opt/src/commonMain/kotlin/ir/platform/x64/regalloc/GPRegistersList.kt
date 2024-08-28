package ir.platform.x64.regalloc

import asm.x64.*
import ir.platform.x64.CallConvention


internal class GPRegistersList(argumentValue: List<GPRegister>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue).toMutableList()
    fun pickRegister(): GPRegister? {
        return freeRegisters.removeLastOrNull()
    }

    fun returnRegister(reg: GPRegister) {
        if (freeRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}