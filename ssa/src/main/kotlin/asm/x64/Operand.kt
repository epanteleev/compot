package asm.x64

interface AnyOperand {
    fun toString(size: Int): String
}

interface Operand : AnyOperand

interface Register : Operand {
    val isCallERSave: Boolean
    val isCallEESave: Boolean
    val isArgument: Boolean
}

enum class GPRegister : Register {
    rax {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false

        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rax"
                4 -> "%eax"
                2 -> "%ax"
                1 -> "%al"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rbx {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false

        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rbx"
                4 -> "%ebx"
                2 -> "%bx"
                1 -> "%bl"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rcx {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true

        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rcx"
                4 -> "%ecx"
                2 -> "%cx"
                1 -> "%cl"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rsi {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true

        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rsi"
                4 -> "%esi"
                2 -> "%si"
                1 -> "%sil"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rdi {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rdi"
                4 -> "%edi"
                2 -> "%di"
                1 -> "%dil"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rdx {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rdx"
                4 -> "%edx"
                2 -> "%dx"
                1 -> "%dl"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rbp {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rbp"
                4 -> "%ebp"
                2 -> "%bp"
                1 -> "%bpl"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r8 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r8"
                4 -> "%r8d"
                2 -> "%r8w"
                1 -> "%r8b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r9 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r9"
                4 -> "%r9d"
                2 -> "%r9w"
                1 -> "%r9b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r10 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r10"
                4 -> "%r10d"
                2 -> "%r10w"
                1 -> "%r10b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r11 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r11"
                4 -> "%r11d"
                2 -> "%r11w"
                1 -> "%r11b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r12 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r12"
                4 -> "%r12d"
                2 -> "%r12w"
                1 -> "%r12b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r13 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r13"
                4 -> "%r13d"
                2 -> "%r13w"
                1 -> "%r13b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r14 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r14"
                4 -> "%r14d"
                2 -> "%r14w"
                1 -> "%r14b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    r15 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r15"
                4 -> "%r15d"
                2 -> "%r15w"
                1 -> "%r15b"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    },

    rsp {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rsp"
                4 -> "%esp"
                else -> throw RuntimeException("Internal error")
            }
            return string
        }
    }
}

interface FPURegister : Register {
    companion object {
        val xmm0 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm0"
                    16 -> "%xmm0"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm1 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm1"
                    16 -> "%xmm1"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm2 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm2"
                    16 -> "%xmm2"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm3 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm3"
                    16 -> "%xmm3"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm4 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm4"
                    16 -> "%xmm4"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm5 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm5"
                    16 -> "%xmm5"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm6 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm6"
                    16 -> "%xmm6"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }

        val xmm7 = object : FPURegister {
            override val isCallEESave: Boolean = TODO()
            override val isCallERSave: Boolean = TODO()
            override val isArgument: Boolean = TODO()
            override fun toString(size: Int): String {
                val string = when (size) {
                    32 -> "%ymm7"
                    16 -> "%xmm7"
                    else -> throw RuntimeException("Internal error")
                }
                return string
            }
        }
    }
}


interface Address : Operand {
    companion object {
        fun mem(base: GPRegister, offset: Long): Address {
            return Address2(base, offset)
        }

        fun mem(base: GPRegister?, offset: Long, index: GPRegister, disp: Long): Address {
            return Address4(base, offset, index, disp)
        }

        fun mem(label: String): Address {
            return AddressLiteral(label)
        }
    }
}

class Address2 internal constructor(val base: GPRegister, val offset: Long) : Address {
    override fun toString(): String {
        return if (offset == 0L) {
            "($base)"
        } else {
            "$offset($base)"
        }
    }

    override fun toString(size: Int): String {
        return if (offset == 0L) {
            "(${base.toString(8)})"
        } else {
            "$offset(${base.toString(8)})"
        }
    }

    override fun hashCode(): Int {
        return base.hashCode() xor offset.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Address2

        if (base != other.base) return false
        if (offset != other.offset) return false
        return true
    }
}

class Address4 internal constructor(private val base: GPRegister?, private val offset: Long, val index: GPRegister, val disp: Long) :
    Address {
    override fun toString(): String {
        return if (offset == 0L) {
            "($base, $index, $disp)"
        } else {
            "$offset($base, $index, $disp)"
        }
    }

    private fun base(size: Int): String {
        return base?.toString(size) ?: ""
    }
    override fun toString(size: Int): String {
        return if (offset == 0L) {
            "(${base(8)}, ${index.toString(8)}, $disp)"
        } else {
            "$offset(${base(8)}, ${index.toString(8)}, $disp)"
        }
    }
}

class ArgumentSlot(val base: GPRegister, val offset: Long) : Address {
    override fun toString(size: Int): String {
        return if (offset == 0L) {
            "(${base.toString(8)})"
        } else {
            "$offset(${base.toString(8)})"
        }
    }
}

data class AddressLiteral internal constructor(val label: String) : Address {
    override fun toString(): String {
        return "$label(%rip)"
    }

    override fun toString(size: Int): String {
        return toString()
    }
}

data class Imm(val value: Long) : AnyOperand {
    override fun toString(): String {
        return "$$value"
    }

    override fun toString(size: Int): String {
        return toString()
    }
}