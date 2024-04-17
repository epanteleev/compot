package ir.platform.x64.codegen.utils

import asm.x64.*


object ApplyClosure {
    operator fun invoke(dst: Operand, first: Operand, second: Operand, closure: GPOperandsVisitorBinaryOp) {
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

    operator fun invoke(dst: Operand, first: Operand, second: Operand, closure: XmmOperandsVisitorBinaryOp) {
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

    operator fun invoke(dst: Operand, src: Operand, closure: GPOperandsVisitorUnaryOp) {
        when (dst) {
            is GPRegister -> {
                when (src) {
                    is GPRegister -> closure.rr(dst, src)
                    is Address    -> closure.ra(dst, src)
                    is ImmInt     -> closure.ri(dst, src.asImm32())
                    else -> closure.default(dst, src)
                }
            }
            is Address -> {
                when (src) {
                    is GPRegister -> closure.ar(dst, src)
                    is Address    -> closure.aa(dst, src)
                    is ImmInt     -> closure.ai(dst, src.asImm32())
                    else -> closure.default(dst, src)
                }
            }
            else -> closure.default(dst, src)
        }
    }

    operator fun invoke(dst: Operand, src: Operand, closure: XmmOperandsVisitorUnaryOp) {
        when (dst) {
            is XmmRegister -> {
                when (src) {
                    is XmmRegister -> closure.rrF(dst, src)
                    is Address     -> closure.raF(dst, src)
                    else           -> closure.default(dst, src)
                }
            }
            is Address -> {
                when (src) {
                    is XmmRegister -> closure.arF(dst, src)
                    is Address     -> closure.aaF(dst, src)
                    else           -> closure.default(dst, src)
                }
            }
            else -> closure.default(dst, src)
        }
    }

    operator fun invoke(dst: Operand, src: Operand, closure: XmmToGPOperandsVisitor) {
        when (dst) {
            is GPRegister -> {
                when (src) {
                    is XmmRegister -> closure.rx(dst, src)
                    is Address     -> closure.ra(dst, src)
                    else           -> closure.default(dst, src)
                }
            }
            is Address -> {
                when (src) {
                    is XmmRegister -> closure.ax(dst, src)
                    is Address     -> closure.aa(dst, src)
                    else           -> closure.default(dst, src)
                }
            }
            else -> closure.default(dst, src)
        }
    }
}