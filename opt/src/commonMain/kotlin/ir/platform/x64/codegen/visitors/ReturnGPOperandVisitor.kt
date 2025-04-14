package ir.platform.x64.codegen.visitors

import asm.x64.Operand
import asm.x64.*


interface ReturnGPOperandVisitor {
    fun rr(dst: GPRegister, src: GPRegister)
    fun ra(dst: GPRegister, src: Address)
    fun ri(dst: GPRegister, src: Imm)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: ReturnGPOperandVisitor) = when (dst) {
            is GPRegister -> when (src) {
                is GPRegister -> closure.rr(dst, src)
                is Address    -> closure.ra(dst, src)
                is Imm      -> closure.ri(dst, src)
                else -> closure.default(dst, src)
            }
            else -> closure.default(dst, src)
        }
    }
}