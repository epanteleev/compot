package ir.platform.x64.utils

import asm.x64.*


object ApplyClosureBinaryOp {
    operator fun invoke(dst: AnyOperand, first: AnyOperand, second: AnyOperand, closure: GPOperandVisitorBinaryOp) {
        when (dst) {
            is GPRegister -> {
                when (first) {
                    is GPRegister -> {
                        when (second) {
                            is GPRegister -> closure.rrr(dst, first, second)
                            is Address    -> closure.rra(dst, first, second)
                            is ImmInt     -> closure.rri(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is GPRegister -> closure.rar(dst, first, second)
                            is Address    -> closure.raa(dst, first, second)
                            is ImmInt     -> closure.rai(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is ImmInt -> {
                        when (second) {
                            is GPRegister -> closure.rir(dst, first, second)
                            is Address    -> closure.ria(dst, first, second)
                            is ImmInt     -> closure.rii(dst, first, second)
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
                            is ImmInt     -> closure.ari(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is GPRegister -> closure.aar(dst, first, second)
                            is Address    -> closure.aaa(dst, first, second)
                            is ImmInt     -> closure.aai(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is ImmInt -> {
                        when (second) {
                            is GPRegister -> closure.air(dst, first, second)
                            is Address    -> closure.aia(dst, first, second)
                            is ImmInt     -> closure.aii(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    else -> closure.error(dst, first, second)
                }
            }
            else -> closure.error(dst, first, second)
        }
    }

    operator fun invoke(dst: AnyOperand, first: AnyOperand, second: AnyOperand, closure: XmmOperandVisitor) {
        when (dst) {
            is XmmRegister -> {
                when (first) {
                    is XmmRegister -> {
                        when (second) {
                            is XmmRegister -> closure.rrrF(dst, first, second)
                            is Address     -> closure.rraF(dst, first, second)
                            is ImmFp       -> closure.rriF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is XmmRegister -> closure.rarF(dst, first, second)
                            is Address     -> closure.raaF(dst, first, second)
                            is ImmFp       -> closure.raiF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is ImmFp -> {
                        when (second) {
                            is XmmRegister -> closure.rirF(dst, first, second)
                            is Address     -> closure.riaF(dst, first, second)
                            is ImmFp       -> closure.riiF(dst, first, second)
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
                            is ImmFp       -> closure.ariF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is Address -> {
                        when (second) {
                            is XmmRegister -> closure.aarF(dst, first, second)
                            is Address     -> closure.aaaF(dst, first, second)
                            is ImmFp       -> closure.aaiF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    is ImmFp -> {
                        when (second) {
                            is XmmRegister -> closure.airF(dst, first, second)
                            is Address     -> closure.aiaF(dst, first, second)
                            is ImmFp       -> closure.aiiF(dst, first, second)
                            else -> closure.error(dst, first, second)
                        }
                    }
                    else -> closure.error(dst, first, second)
                }
            }
            else -> closure.error(dst, first, second)
        }
    }
}