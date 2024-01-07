package ir.platform.x64.codegen.utils

import asm.x64.*

interface GPOperandVisitorBinaryOp {
    fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister)
    fun arr(dst: Address, first: GPRegister, second: GPRegister)
    fun rar(dst: GPRegister, first: Address, second: GPRegister)
    fun rir(dst: GPRegister, first: Imm32, second: GPRegister)
    fun rra(dst: GPRegister, first: GPRegister, second: Address)
    fun rri(dst: GPRegister, first: GPRegister, second: Imm32)
    fun raa(dst: GPRegister, first: Address, second: Address)
    fun rii(dst: GPRegister, first: Imm32, second: Imm32)
    fun ria(dst: GPRegister, first: Imm32, second: Address)
    fun rai(dst: GPRegister, first: Address, second: Imm32)
    fun ara(dst: Address, first: GPRegister, second: Address)
    fun aii(dst: Address, first: Imm32, second: Imm32)
    fun air(dst: Address, first: Imm32, second: GPRegister)
    fun aia(dst: Address, first: Imm32, second: Address)
    fun ari(dst: Address, first: Register, second: Imm32)
    fun aai(dst: Address, first: Address, second: Imm32)
    fun aar(dst: Address, first: Address, second: GPRegister)
    fun aaa(dst: Address, first: Address, second: Address)
    fun default(dst: Operand, first: Operand, second: Operand)
}