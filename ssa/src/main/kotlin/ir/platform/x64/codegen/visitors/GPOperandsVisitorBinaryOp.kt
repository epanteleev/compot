package ir.platform.x64.codegen.visitors

import asm.x64.*

interface GPOperandsVisitorBinaryOp {
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
    fun ari(dst: Address, first: GPRegister, second: Imm32)
    fun aai(dst: Address, first: Address, second: Imm32)
    fun aar(dst: Address, first: Address, second: GPRegister)
    fun aaa(dst: Address, first: Address, second: Address)
    fun default(dst: Operand, first: Operand, second: Operand)

    companion object {
        fun apply(dst: Operand, first: Operand, second: Operand, closure: GPOperandsVisitorBinaryOp) {
            when (dst) {
                is GPRegister -> {
                    when (first) {
                        is GPRegister -> {
                            when (second) {
                                is GPRegister -> closure.rrr(dst, first, second)
                                is Address    -> closure.rra(dst, first, second)
                                is ImmInt     -> closure.rri(dst, first, second.asImm32())
                                else -> closure.default(dst, first, second)
                            }
                        }
                        is Address -> {
                            when (second) {
                                is GPRegister -> closure.rar(dst, first, second)
                                is Address    -> closure.raa(dst, first, second)
                                is ImmInt     -> closure.rai(dst, first, second.asImm32())
                                else -> closure.default(dst, first, second)
                            }
                        }
                        is ImmInt-> {
                            when (second) {
                                is GPRegister -> closure.rir(dst, first.asImm32(), second)
                                is Address    -> closure.ria(dst, first.asImm32(), second)
                                is ImmInt     -> closure.rii(dst, first.asImm32(), second.asImm32())
                                else -> closure.default(dst, first, second)
                            }
                        }
                        else -> closure.default(dst, first, second)
                    }
                }
                is Address -> {
                    when (first) {
                        is GPRegister -> {
                            when (second) {
                                is GPRegister -> closure.arr(dst, first, second)
                                is Address    -> closure.ara(dst, first, second)
                                is ImmInt     -> closure.ari(dst, first, second.asImm32())
                                else -> closure.default(dst, first, second)
                            }
                        }
                        is Address -> {
                            when (second) {
                                is GPRegister -> closure.aar(dst, first, second)
                                is Address    -> closure.aaa(dst, first, second)
                                is ImmInt     -> closure.aai(dst, first, second.asImm32())
                                else -> closure.default(dst, first, second)
                            }
                        }
                        is ImmInt -> {
                            when (second) {
                                is GPRegister -> closure.air(dst, first.asImm32(), second)
                                is Address    -> closure.aia(dst, first.asImm32(), second)
                                is ImmInt     -> closure.aii(dst, first.asImm32(), second.asImm32())
                                else -> closure.default(dst, first, second)
                            }
                        }
                        else -> closure.default(dst, first, second)
                    }
                }
                else -> closure.default(dst, first, second)
            }
        }
    }
}