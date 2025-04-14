package ir.platform.x64.codegen.visitors

import asm.x64.Operand
import asm.x64.*

interface CmpGPOperandVisitor {
    fun rr(first: GPRegister, second: GPRegister)
    fun ra(first: GPRegister, second: Address)
    fun ar(first: Address, second: GPRegister)
    fun aa(first: Address, second: Address)
    fun ri(first: GPRegister, second: Imm)
    fun ai(first: Address, second: Imm)
    fun ia(first: Imm, second: Address)
    fun ir(first: Imm, second: GPRegister)
    fun ii(first: Imm, second: Imm)
    fun default(first: Operand, second: Operand)

    companion object {
        fun apply(first: Operand, second: Operand, closure: CmpGPOperandVisitor) = when (first) {
            is GPRegister -> when (second) {
                is GPRegister -> closure.rr(first, second)
                is Address    -> closure.ra(first, second)
                is Imm        -> closure.ri(first, second)
                else -> closure.default(first, second)
            }
            is Address -> when (second) {
                is GPRegister -> closure.ar(first, second)
                is Address    -> closure.aa(first, second)
                is Imm        -> closure.ai(first, second)
                else -> closure.default(first, second)
            }
            is Imm -> when (second) {
                is GPRegister -> closure.ir(first, second)
                is Address    -> closure.ia(first, second)
                is Imm        -> closure.ii(first, second)
                else -> closure.default(first, second)
            }
            else -> closure.default(first, second)
        }
    }
}