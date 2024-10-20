package ir.platform.x64.codegen.visitors

import asm.Operand
import asm.x64.*


interface XmmToGPOperandsVisitor {
    fun rx(dst: GPRegister, src: XmmRegister)
    fun ax(dst: Address, src: XmmRegister)
    fun ra(dst: GPRegister, src: Address)
    fun aa(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: XmmToGPOperandsVisitor) {
            when (dst) {
                is GPRegister -> {
                    when (src) {
                        is XmmRegister -> closure.rx(dst, src)
                        is Address     -> closure.ra(dst, src)
                        else           -> closure.default(dst, src)
                    }
                }
                is Address -> {
                    when (src) {
                        is XmmRegister -> closure.ax(dst, src)
                        is Address     -> closure.aa(dst, src)
                        else           -> closure.default(dst, src)
                    }
                }
                else -> closure.default(dst, src)
            }
        }
    }
}