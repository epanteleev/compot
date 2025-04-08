package ir.platform.x64.codegen.visitors

import asm.x64.Operand
import asm.x64.*


interface CallXmmOperandValueVisitor {
    fun rr(dst: XmmRegister, src: XmmRegister)
    fun ar(dst: Address, src: XmmRegister)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: CallXmmOperandValueVisitor) = when (dst) {
            is XmmRegister -> when (src) {
                is XmmRegister -> closure.rr(dst, src)
                else -> closure.default(dst, src)
            }
            is Address -> when (src) {
                is XmmRegister -> closure.ar(dst, src)
                else -> closure.default(dst, src)
            }
            else -> closure.default(dst, src)
        }
    }
}