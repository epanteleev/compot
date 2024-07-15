package ir.platform.x64.codegen.visitors

import asm.x64.*

interface XmmOperandsVisitorUnaryOp {
    fun rrF(dst: XmmRegister, src: XmmRegister)
    fun raF(dst: XmmRegister, src: Address)
    fun arF(dst: Address, src: XmmRegister)
    fun aaF(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: XmmOperandsVisitorUnaryOp) {
            when (dst) {
                is XmmRegister -> {
                    when (src) {
                        is XmmRegister -> closure.rrF(dst, src)
                        is Address     -> closure.raF(dst, src)
                        else           -> closure.default(dst, src)
                    }
                }
                is Address -> {
                    when (src) {
                        is XmmRegister -> closure.arF(dst, src)
                        is Address     -> closure.aaF(dst, src)
                        else           -> closure.default(dst, src)
                    }
                }
                else -> closure.default(dst, src)
            }
        }
    }
}