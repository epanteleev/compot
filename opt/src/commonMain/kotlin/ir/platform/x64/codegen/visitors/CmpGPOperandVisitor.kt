package ir.platform.x64.codegen.visitors

import asm.x64.*

interface CmpGPOperandVisitor {
    fun rr(first: GPRegister, second: GPRegister)
    fun ra(first: GPRegister, second: Address)
    fun ar(first: Address, second: GPRegister)
    fun aa(first: Address, second: Address)
    fun ri(first: GPRegister, second: Imm32)
    fun ai(first: Address, second: Imm32)
    fun ia(first: Imm32, second: Address)
    fun ir(first: Imm32, second: GPRegister)
    fun ii(first: Imm32, second: Imm32)
    fun default(first: Operand, second: Operand)

    companion object {
        fun apply(first: Operand, second: Operand, closure: CmpGPOperandVisitor) = when (first) {
            is GPRegister -> when (second) {
                is GPRegister -> closure.rr(first, second)
                is Address    -> closure.ra(first, second)
                is ImmInt     -> closure.ri(first, second.asImm32())
                else -> closure.default(first, second)
            }
            is Address -> when (second) {
                is GPRegister -> closure.ar(first, second)
                is Address    -> closure.aa(first, second)
                is ImmInt     -> closure.ai(first, second.asImm32())
                else -> closure.default(first, second)
            }
            is ImmInt -> when (second) {
                is GPRegister -> closure.ir(first.asImm32(), second)
                is Address    -> closure.ia(first.asImm32(), second)
                is ImmInt     -> closure.ii(first.asImm32(), second.asImm32())
                else -> closure.default(first, second)
            }
            else -> closure.default(first, second)
        }
    }
}