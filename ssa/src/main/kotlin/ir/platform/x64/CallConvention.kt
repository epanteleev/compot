package ir.platform.x64

import asm.x64.*
import asm.x64.GPRegister.*
import asm.x64.XmmRegister.*

// Calling conventions for different C++ compilers and operating systems
// https://www.agner.org/optimize/calling_conventions.pdf
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

    val xmmArgumentRegister: Array<XmmRegister> = arrayOf(
        xmm0,
        xmm1,
        xmm2,
        xmm3,
        xmm4,
        xmm5,
        xmm6,
        xmm7,
    )

    val xmmRegisters: Array<XmmRegister> = arrayOf(
        xmm0,
        xmm1,
        xmm2,
        xmm3,
        xmm4,
        xmm5,
        xmm6,
        xmm7,
        xmm8,
        xmm9,
        xmm10,
        xmm11,
        xmm12,
        xmm13,
        xmm14,
        xmm15,
    )

    val temp1 = rax
    val temp2 = r10
    val retReg = rax

    val xmmTemp1 = xmm8
    val fpRet    = xmm0

    fun availableRegisters(usedArgumentRegisters: List<GPRegister>): Set<GPRegister> {
        val allRegisters = mutableSetOf<GPRegister>()
        allRegisters.addAll(gpCallerSaveRegs)
        allRegisters.addAll(gpCalleeSaveRegs)
        allRegisters.addAll(gpArgumentRegisters)

        allRegisters.remove(rbp)
        allRegisters.removeAll(usedArgumentRegisters.toSet())
        allRegisters.remove(temp1)
        allRegisters.remove(temp2)
        allRegisters.remove(retReg)
        return allRegisters
    }

    fun availableXmmRegisters(usedArgumentRegisters: List<XmmRegister>): Set<XmmRegister> {
        val allRegisters = mutableSetOf<XmmRegister>()
        allRegisters.addAll(xmmRegisters)
        allRegisters.remove(fpRet)
        allRegisters.remove(xmmTemp1)
        allRegisters.removeAll(usedArgumentRegisters.toSet())
        return allRegisters
    }

    const val CONSTANT_POOL_PREFIX = ".LCP_"
    const val FLOAT_SUB_ZERO_SYMBOL = ".LCP_FLTSZ"
    const val DOUBLE_SUB_ZERO_SYMBOL = ".LCP_DBLTSZ"

    const val STACK_ALIGNMENT = 16L
}