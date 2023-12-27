package ir.platform.x64.utils

import asm.x64.*

interface XmmOperandVisitor {
    fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister)
    fun arrF(dst: Address, first: XmmRegister, second: XmmRegister)
    fun rarF(dst: XmmRegister, first: Address, second: XmmRegister)
    fun rirF(dst: XmmRegister, first: ImmFp, second: XmmRegister)
    fun rraF(dst: XmmRegister, first: XmmRegister, second: Address)
    fun rriF(dst: XmmRegister, first: XmmRegister, second: ImmFp)
    fun raaF(dst: XmmRegister, first: Address, second: Address)
    fun riiF(dst: XmmRegister, first: ImmFp, second: ImmFp)
    fun riaF(dst: XmmRegister, first: ImmFp, second: Address)
    fun raiF(dst: XmmRegister, first: Address, second: ImmFp)
    fun araF(dst: Address, first: XmmRegister, second: Address)
    fun aiiF(dst: Address, first: ImmFp, second: ImmFp)
    fun airF(dst: Address, first: ImmFp, second: XmmRegister)
    fun aiaF(dst: Address, first: ImmFp, second: Address)
    fun ariF(dst: Address, first: Register, second: ImmFp)
    fun aaiF(dst: Address, first: Address, second: ImmFp)
    fun aarF(dst: Address, first: Address, second: XmmRegister)
    fun aaaF(dst: Address, first: Address, second: Address)
    fun error(dst: AnyOperand, first: AnyOperand, second: AnyOperand)
}