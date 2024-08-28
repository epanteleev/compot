package ir.platform.x64.regalloc

import asm.x64.*
import ir.platform.x64.CallConvention


internal class XmmRegisterList(arguments: List<XmmRegister>) {
    private var freeRegisters = CallConvention.availableXmmRegisters(arguments).toMutableList()

    fun pickRegister(): XmmRegister? {
        if (freeRegisters.isEmpty()) {
            return null
        }

        return freeRegisters.removeLast()
    }

    fun returnRegister(reg: XmmRegister) {
        if (freeRegisters.contains(reg)) {
            return
        }

        freeRegisters.add(reg)
    }
}