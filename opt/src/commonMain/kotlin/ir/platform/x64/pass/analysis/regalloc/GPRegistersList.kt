package ir.platform.x64.pass.analysis.regalloc

import asm.x64.*
import ir.platform.x64.CallConvention


internal class GPRegistersList(usedArgumentRegisters: List<GPRegister>) {
    private var freeRegisters = CallConvention.availableRegisters(usedArgumentRegisters).toMutableList()

    private val usedCalleeSaveRegisters = hashSetOf<GPRegister>(GPRegister.rbx) //NOTE: rbx is temporal and callee-save register

    init {
        for (reg in usedArgumentRegisters) {
            if (CallConvention.gpCalleeSaveRegs.contains(reg)) {
                usedCalleeSaveRegisters.add(reg)
            }
        }
    }

    fun usedCalleeSaveRegisters(): List<GPRegister> {
        return usedCalleeSaveRegisters.toList()
    }

    private fun tryAddCalleeSaveRegister(register: GPRegister) {
        if (!CallConvention.gpCalleeSaveRegs.contains(register)) {
            return
        }

        usedCalleeSaveRegisters.add(register)
    }

    fun pickRegister(excludeIf: (GPRegister) -> Boolean): GPRegister? {
        val reg = freeRegisters.findLast { !excludeIf(it) }
        if (reg == null) {
            return null
        }
        freeRegisters.remove(reg)
        tryAddCalleeSaveRegister(reg)
        return reg
    }

    fun returnRegister(reg: GPRegister) {
        if (freeRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}