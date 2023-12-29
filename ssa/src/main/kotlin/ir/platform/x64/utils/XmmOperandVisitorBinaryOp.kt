package ir.platform.x64.utils

import asm.x64.*

interface XmmOperandVisitorBinaryOp {
    fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister)
    fun arrF(dst: Address, first: XmmRegister, second: XmmRegister)
    fun rarF(dst: XmmRegister, first: Address, second: XmmRegister)
    fun rraF(dst: XmmRegister, first: XmmRegister, second: Address)
    fun raaF(dst: XmmRegister, first: Address, second: Address)
    fun araF(dst: Address, first: XmmRegister, second: Address)
    fun aarF(dst: Address, first: Address, second: XmmRegister)
    fun aaaF(dst: Address, first: Address, second: Address)
    fun error(dst: AnyOperand, first: AnyOperand, second: AnyOperand)
}