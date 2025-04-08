package ir.platform.x64.codegen.visitors

import asm.x64.Operand
import asm.x64.*


interface GPOperandsVisitorUnaryOp {
    fun rr(dst: GPRegister, src: GPRegister)
    fun ra(dst: GPRegister, src: Address)
    fun ar(dst: Address, src: GPRegister)
    fun aa(dst: Address, src: Address)
    fun ri(dst: GPRegister, src: Imm32)
    fun ai(dst: Address, src: Imm32)
    fun default(dst: Operand, src: Operand)

    companion object {
        fun apply(dst: Operand, src: Operand, closure: GPOperandsVisitorUnaryOp) {
            when (dst) {
                is GPRegister -> {
                    when (src) {
                        is GPRegister -> closure.rr(dst, src)
                        is Address    -> closure.ra(dst, src)
                        is ImmInt     -> closure.ri(dst, src.asImm32())
                        else -> closure.default(dst, src)
                    }
                }
                is Address -> {
                    when (src) {
                        is GPRegister -> closure.ar(dst, src)
                        is Address    -> closure.aa(dst, src)
                        is ImmInt     -> closure.ai(dst, src.asImm32())
                        else -> closure.default(dst, src)
                    }
                }
                else -> closure.default(dst, src)
            }
        }
    }
}