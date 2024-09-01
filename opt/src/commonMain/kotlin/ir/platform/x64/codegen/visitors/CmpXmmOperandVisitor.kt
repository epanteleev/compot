package ir.platform.x64.codegen.visitors

import asm.x64.*

interface CmpXmmOperandVisitor {
    fun rr(first: XmmRegister, second: XmmRegister)
    fun ra(first: XmmRegister, second: Address)
    fun ar(first: Address, second: XmmRegister)
    fun aa(first: Address, second: Address)
    fun default(first: Operand, second: Operand)

    companion object {
        fun apply(first: Operand, second: Operand, closure: CmpXmmOperandVisitor) = when (first) {
            is XmmRegister -> when (second) {
                is XmmRegister -> closure.rr(first, second)
                is Address    -> closure.ra(first, second)
                else -> closure.default(first, second)
            }
            is Address -> when (second) {
                is XmmRegister -> closure.ar(first, second)
                is Address    -> closure.aa(first, second)
                else -> closure.default(first, second)
            }
            else -> closure.default(first, second)
        }
    }
}