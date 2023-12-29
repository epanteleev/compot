package ir.platform.x64.utils

import asm.x64.*


object ApplyClosure {
    operator fun invoke(dst: AnyOperand, first: AnyOperand, second: AnyOperand, closure: GPOperandVisitorBinaryOp) {
        when (dst) {
            is GPRegister -> {
                when (first) {
                    is GPRegister -> {
                        when (second) {
                            is GPRegister -> closure.rrr(dst, first, second)
                            is Address    -> closure.rra(dst, first, second)
                            is Imm32      -> closure.rri(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is GPRegister -> closure.rar(dst, first, second)
                            is Address    -> closure.raa(dst, first, second)
                            is Imm32      -> closure.rai(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Imm32-> {
                        when (second) {
                            is GPRegister -> closure.rir(dst, first, second)
                            is Address    -> closure.ria(dst, first, second)
                            is Imm32      -> closure.rii(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    else -> closure.error(dst, first, second)
                }
            }
            is Address -> {
                when (first) {
                    is GPRegister -> {
                        when (second) {
                            is GPRegister -> closure.arr(dst, first, second)
                            is Address    -> closure.ara(dst, first, second)
                            is Imm32      -> closure.ari(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is GPRegister -> closure.aar(dst, first, second)
                            is Address    -> closure.aaa(dst, first, second)
                            is Imm32      -> closure.aai(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Imm32 -> {
                        when (second) {
                            is GPRegister -> closure.air(dst, first, second)
                            is Address    -> closure.aia(dst, first, second)
                            is Imm32     -> closure.aii(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    else -> closure.error(dst, first, second)
                }
            }
            else -> closure.error(dst, first, second)
        }
    }

    operator fun invoke(dst: AnyOperand, first: AnyOperand, second: AnyOperand, closure: XmmOperandVisitorBinaryOp) {
        when (dst) {
            is XmmRegister -> {
                when (first) {
                    is XmmRegister -> {
                        when (second) {
                            is XmmRegister -> closure.rrrF(dst, first, second)
                            is Address     -> closure.rraF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is XmmRegister -> closure.rarF(dst, first, second)
                            is Address     -> closure.raaF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    else -> closure.error(dst, first, second)
                }
            }
            is Address -> {
                when (first) {
                    is XmmRegister -> {
                        when (second) {
                            is XmmRegister -> closure.arrF(dst, first, second)
                            is Address     -> closure.araF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is XmmRegister -> closure.aarF(dst, first, second)
                            is Address     -> closure.aaaF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    else -> closure.error(dst, first, second)
                }
            }
            else -> closure.error(dst, first, second)
        }
    }

    operator fun invoke(dst: AnyOperand, src: AnyOperand, closure: GPOperandVisitorUnaryOp) {
        when (dst) {
            is GPRegister -> {
                when (src) {
                    is GPRegister -> closure.rr(dst, src)
                    is Address    -> closure.ra(dst, src)
                    is Imm32      -> closure.ri(dst, src)
                    else -> closure.error(src, dst)
                }
            }
            is Address -> {
                when (src) {
                    is GPRegister -> closure.ar(dst, src)
                    is Address    -> closure.aa(dst, src)
                    is Imm32      -> closure.ai(dst, src)
                    else -> closure.error(src, dst)
                }
            }
            else -> closure.error(src, dst)
        }
    }

    operator fun invoke(dst: AnyOperand, src: AnyOperand, closure: XmmOperandVisitorUnaryOp) {
        when (dst) {
            is XmmRegister -> {
                when (src) {
                    is XmmRegister -> closure.rrF(dst, src)
                    is Address     -> closure.raF(dst, src)
                    else -> closure.error(src, dst)
                }
            }
            is Address -> {
                when (src) {
                    is XmmRegister -> closure.arF(dst, src)
                    is Address     -> closure.aaF(dst, src)
                    else -> closure.error(src, dst)
                }
            }
            else -> closure.error(src, dst)
        }
    }
}