package ir.platform.x64.pass.analysis.regalloc

import asm.x64.*
import ir.platform.x64.CallConvention


internal class XmmRegisterList(arguments: List<XmmRegister>) {
    private var freeRegisters = CallConvention.availableXmmRegisters(arguments).toMutableList()

    fun pickRegister(excludeIf: (Register) -> Boolean): XmmRegister? {
        return freeRegisters.findLast { !excludeIf(it) }?.also { freeRegisters.remove(it) }
    }

    fun returnRegister(reg: XmmRegister) {
        if (freeRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}