package ir.platform.x64.codegen.visitors

import asm.x64.*


interface ReturnXmmOperandVisitor {
    fun rr(dst: XmmRegister, src: XmmRegister)
    fun ra(dst: XmmRegister, src: Address)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: ReturnXmmOperandVisitor) = when (dst) {
            is XmmRegister -> when (src) {
                is XmmRegister -> closure.rr(dst, src)
                is Address     -> closure.ra(dst, src)
                else -> closure.default(dst, src)
            }
            else -> closure.default(dst, src)
        }
    }
}