package ir.platform.x64.codegen.visitors

import asm.x64.*


interface GPOperandToXmmVisitor {
    fun rx(dst: XmmRegister, src: GPRegister)
    fun ax(dst: XmmRegister, src: Address)
    fun ar(dst: Address, src: GPRegister)
    fun aa(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: GPOperandToXmmVisitor) {
            when (dst) {
                is XmmRegister -> {
                    when (src) {
                        is GPRegister -> closure.rx(dst, src)
                        is Address    -> closure.ax(dst, src)
                        else -> closure.default(dst, src)
                    }
                }
                is Address -> {
                    when (src) {
                        is GPRegister -> closure.ar(dst, src)
                        is Address    -> closure.aa(dst, src)
                        else -> closure.default(dst, src)
                    }
                }
                else -> closure.default(dst, src)
            }
        }
    }
}