package ir.codegen.x64

import asm.x64.*

object CallConvention {
    internal val availableRegisters: ArrayList<GPRegister> by lazy { allRegisters() }

    val gpArgumentRegisters: Array<GPRegister> = arrayOf(
        Rdi.rdi,
        Rsi.rsi,
        Rdx.rdx,
        Rcx.rcx,
        R8.r8,
        R9.r9
    )

    val gpCallerSaveRegs: Array<GPRegister> = arrayOf(
        Rax.rax,
        Rcx.rcx,
        Rdx.rdx,
        Rsi.rsi,
        Rdi.rdi,
        R8.r8,
        R9.r9,
        R10.r10,
        R11.r11
    )

    val gpCalleeSaveRegs: Array<GPRegister> = arrayOf(
        Rbp.rbp,
        Rbx.rbx,
        R12.r12,
        R13.r13,
        R14.r14,
        R15.r15
    )

    fun temp1(size: Int): GPRegister {
        return Rax(size)
    }

    fun temp2(size: Int): GPRegister {
        return R10(size)
    }

    private fun allRegisters(): ArrayList<GPRegister> {
        val allRegisters = arrayListOf<GPRegister>()
        allRegisters.addAll(gpCallerSaveRegs)
        allRegisters.addAll(gpCalleeSaveRegs)

        allRegisters.remove(Rbp.rbp)
        allRegisters.removeAll(gpArgumentRegisters.toSet())
        allRegisters.remove(temp1(8))
        allRegisters.remove(temp2(8))
        return allRegisters
    }
}