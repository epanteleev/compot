package ir.platform.x64.codegen.visitors

import asm.x64.*

interface XmmOperandsVisitorBinaryOp {
    fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister)
    fun arrF(dst: Address, first: XmmRegister, second: XmmRegister)
    fun rarF(dst: XmmRegister, first: Address, second: XmmRegister)
    fun rraF(dst: XmmRegister, first: XmmRegister, second: Address)
    fun raaF(dst: XmmRegister, first: Address, second: Address)
    fun araF(dst: Address, first: XmmRegister, second: Address)
    fun aarF(dst: Address, first: Address, second: XmmRegister)
    fun aaaF(dst: Address, first: Address, second: Address)
    fun default(dst: Operand, first: Operand, second: Operand)

    companion object {
        fun apply(dst: Operand, first: Operand, second: Operand, closure: XmmOperandsVisitorBinaryOp) {
            when (dst) {
                is XmmRegister -> {
                    when (first) {
                        is XmmRegister -> {
                            when (second) {
                                is XmmRegister -> closure.rrrF(dst, first, second)
                                is Address     -> closure.rraF(dst, first, second)
                                else -> closure.default(dst, first, second)
                            }
                        }
                        is Address -> {
                            when (second) {
                                is XmmRegister -> closure.rarF(dst, first, second)
                                is Address     -> closure.raaF(dst, first, second)
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
                                is XmmRegister -> closure.arrF(dst, first, second)
                                is Address     -> closure.araF(dst, first, second)
                                else -> closure.default(dst, first, second)
                            }
                        }
                        is Address -> {
                            when (second) {
                                is XmmRegister -> closure.aarF(dst, first, second)
                                is Address     -> closure.aaaF(dst, first, second)
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