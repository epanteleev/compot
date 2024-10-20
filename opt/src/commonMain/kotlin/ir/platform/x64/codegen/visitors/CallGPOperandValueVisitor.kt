package ir.platform.x64.codegen.visitors

import asm.Operand
import asm.x64.*


interface CallGPOperandValueVisitor {
    fun rr(dst: GPRegister, src: GPRegister)
    fun ar(dst: Address, src: GPRegister)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: CallGPOperandValueVisitor) = when (dst) {
            is GPRegister -> when (src) {
                is GPRegister -> closure.rr(dst, src)
                else -> closure.default(dst, src)
            }
            is Address -> when (src) {
                is GPRegister -> closure.ar(dst, src)
                else -> closure.default(dst, src)
            }
            else -> closure.default(dst, src)
        }
    }
}