package ir.platform.x64.codegen.visitors

import asm.x64.Operand
import asm.x64.*

interface XmmOperandsVisitorUnaryOp {
    fun rr(dst: XmmRegister, src: XmmRegister)
    fun ra(dst: XmmRegister, src: Address)
    fun ar(dst: Address, src: XmmRegister)
    fun aa(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: XmmOperandsVisitorUnaryOp) = when (dst) {
            is XmmRegister -> when (src) {
                is XmmRegister -> closure.rr(dst, src)
                is Address     -> closure.ra(dst, src)
                else           -> closure.default(dst, src)
            }
            is Address -> when (src) {
                is XmmRegister -> closure.ar(dst, src)
                is Address     -> closure.aa(dst, src)
                else           -> closure.default(dst, src)
            }
            else -> closure.default(dst, src)
        }
    }
}