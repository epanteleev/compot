package ir.platform.x64.pass.analysis.regalloc

import asm.x64.*
import ir.platform.x64.CallConvention


internal class XmmRegisterList(usedArgumentRegisters: List<XmmRegister>) {
    private var freeRegisters = CallConvention.availableXmmRegisters(usedArgumentRegisters).toMutableList()

    private val usedCalleeSaveRegisters = hashSetOf<XmmRegister>()

    init {
        for (reg in usedArgumentRegisters) {
            if (CallConvention.xmmCalleeSaveRegs.contains(reg)) {
                usedCalleeSaveRegisters.add(reg)
            }
        }
    }

    fun usedCalleeSaveRegisters(): List<XmmRegister> {
        return usedCalleeSaveRegisters.toList()
    }

    private fun tryAddCalleeSaveRegister(register: XmmRegister) {
        if (!CallConvention.xmmCalleeSaveRegs.contains(register)) {
            return
        }

        usedCalleeSaveRegisters.add(register)
    }

    fun pickRegister(excludeIf: (XmmRegister) -> Boolean): XmmRegister? {
        val reg = freeRegisters.findLast { !excludeIf(it) }
        if (reg == null) {
            return null
        }
        freeRegisters.remove(reg)
        tryAddCalleeSaveRegister(reg)
        return reg
    }

    fun returnRegister(reg: XmmRegister) {
        if (freeRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}