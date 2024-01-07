package ir.platform.x64.codegen.utils

import asm.x64.*


interface XmmToGPOperandVisitor {
    fun rx(dst: GPRegister, src: XmmRegister)
    fun ax(dst: Address, src: XmmRegister)
    fun ra(dst: GPRegister, src: Address)
    fun aa(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)
}