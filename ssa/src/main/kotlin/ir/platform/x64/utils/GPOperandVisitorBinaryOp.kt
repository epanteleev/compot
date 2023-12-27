package ir.platform.x64.utils

import asm.x64.*

interface GPOperandVisitorBinaryOp {
    fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister)
    fun arr(dst: Address, first: GPRegister, second: GPRegister)
    fun rar(dst: GPRegister, first: Address, second: GPRegister)
    fun rir(dst: GPRegister, first: ImmInt, second: GPRegister)
    fun rra(dst: GPRegister, first: GPRegister, second: Address)
    fun rri(dst: GPRegister, first: GPRegister, second: ImmInt)
    fun raa(dst: GPRegister, first: Address, second: Address)
    fun rii(dst: GPRegister, first: ImmInt, second: ImmInt)
    fun ria(dst: GPRegister, first: ImmInt, second: Address)
    fun rai(dst: GPRegister, first: Address, second: ImmInt)
    fun ara(dst: Address, first: GPRegister, second: Address)
    fun aii(dst: Address, first: ImmInt, second: ImmInt)
    fun air(dst: Address, first: ImmInt, second: GPRegister)
    fun aia(dst: Address, first: ImmInt, second: Address)
    fun ari(dst: Address, first: Register, second: ImmInt)
    fun aai(dst: Address, first: Address, second: ImmInt)
    fun aar(dst: Address, first: Address, second: GPRegister)
    fun aaa(dst: Address, first: Address, second: Address)
    fun error(dst: AnyOperand, first: AnyOperand, second: AnyOperand)
}