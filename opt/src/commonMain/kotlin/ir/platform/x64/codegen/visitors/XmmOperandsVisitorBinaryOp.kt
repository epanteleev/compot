package ir.platform.x64.codegen.visitors

import asm.Operand
import asm.x64.*

interface XmmOperandsVisitorBinaryOp {
    fun rrr(dst: XmmRegister, first: XmmRegister, second: XmmRegister)
    fun arr(dst: Address, first: XmmRegister, second: XmmRegister)
    fun rar(dst: XmmRegister, first: Address, second: XmmRegister)
    fun rra(dst: XmmRegister, first: XmmRegister, second: Address)
    fun raa(dst: XmmRegister, first: Address, second: Address)
    fun ara(dst: Address, first: XmmRegister, second: Address)
    fun aar(dst: Address, first: Address, second: XmmRegister)
    fun aaa(dst: Address, first: Address, second: Address)
    fun default(dst: Operand, first: Operand, second: Operand)

    companion object {
        fun apply(dst: Operand, first: Operand, second: Operand, closure: XmmOperandsVisitorBinaryOp) = when (dst) {
            is XmmRegister -> {
                when (first) {
                    is XmmRegister -> {
                        when (second) {
                            is XmmRegister -> closure.rrr(dst, first, second)
                            is Address     -> closure.rra(dst, first, second)
                            else -> closure.default(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is XmmRegister -> closure.rar(dst, first, second)
                            is Address     -> closure.raa(dst, first, second)
                            else -> closure.default(dst, first, second)
                        }
                    }
                    else -> closure.default(dst, first, second)
                }
            }
            is Address -> {
                when (first) {
                    is XmmRegister -> {
                        when (second) {
                            is XmmRegister -> closure.arr(dst, first, second)
                            is Address     -> closure.ara(dst, first, second)
                            else -> closure.default(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is XmmRegister -> closure.aar(dst, first, second)
                            is Address     -> closure.aaa(dst, first, second)
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