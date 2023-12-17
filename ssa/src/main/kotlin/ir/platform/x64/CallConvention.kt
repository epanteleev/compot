package ir.platform.x64

import asm.x64.*
import asm.x64.GPRegister.*


object CallConvention {
    val gpArgumentRegisters: Array<GPRegister> = arrayOf(
        rdi,
        rsi,
        rdx,
        rcx,
        r8,
        r9
    )

    val gpCallerSaveRegs: Array<GPRegister> = arrayOf(
        rax,
        rcx,
        rdx,
        rsi,
        rdi,
        r8,
        r9,
        r10,
        r11
    )

    val gpCalleeSaveRegs: Array<GPRegister> = arrayOf(
        rbp,
        rbx,
        r12,
        r13,
        r14,
        r15
    )

    val temp1 = rax

    val temp2 = r10

    fun availableRegisters(usedArgumentRegisters: List<GPRegister>): Set<GPRegister> {
        val allRegisters = mutableSetOf<GPRegister>()
        allRegisters.addAll(gpCallerSaveRegs)
        allRegisters.addAll(gpCalleeSaveRegs)
        allRegisters.addAll(gpArgumentRegisters)

        allRegisters.remove(rbp)
        allRegisters.removeAll(usedArgumentRegisters.toSet())
        allRegisters.remove(temp1)
        allRegisters.remove(temp2)
        return allRegisters
    }
}