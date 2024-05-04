package ir.platform.x64.codegen.utils

import asm.x64.*


interface GPOperandToXmmVisitor {
    fun rx(dst: XmmRegister, src: GPRegister)
    fun ax(dst: XmmRegister, src: Address)
    fun ar(dst: Address, src: GPRegister)
    fun aa(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)
}