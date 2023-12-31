package ir.platform.x64.utils

import asm.x64.*

interface XmmOperandVisitorUnaryOp {
    fun rrF(dst: XmmRegister, src: XmmRegister)
    fun raF(dst: XmmRegister, src: Address)
    fun arF(dst: Address, src: XmmRegister)
    fun aaF(dst: Address, src: Address)
    fun default(dst: Operand, src: Operand)
}