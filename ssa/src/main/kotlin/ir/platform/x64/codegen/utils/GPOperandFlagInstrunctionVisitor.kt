package ir.platform.x64.codegen.utils

import asm.x64.Address
import asm.x64.GPRegister
import asm.x64.Operand

interface GPOperandFlagInstrunctionVisitor {
    fun r(dst: GPRegister)
    fun a(dst: Address)
    fun default(dst: Operand)
}