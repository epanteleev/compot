package ir.platform.x64.codegen.utils

import asm.x64.*


interface GPOperandVisitorUnaryOp {
    fun rr(dst: GPRegister, src: GPRegister)
    fun ra(dst: GPRegister, src: Address)
    fun ar(dst: Address, src: GPRegister)
    fun aa(dst: Address, src: Address)
    fun ri(dst: GPRegister, src: Imm32)
    fun ai(dst: Address, src: Imm32)
    fun default(dst: Operand, src: Operand)
}