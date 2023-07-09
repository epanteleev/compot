package asm

import java.lang.RuntimeException

interface Operand {
    val size: Int
}

interface Register: Operand
interface FPURegister: Register
interface GPRegister: Register

class Rax(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rax"
            4 -> "%eax"
            2 -> "%ax"
            1 -> "%al"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rbx(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rbx"
            4 -> "%ebx"
            2 -> "%bx"
            1 -> "%bl"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rcx(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rcx"
            4 -> "%ecx"
            2 -> "%cx"
            1 -> "%cl"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rsi(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rsi"
            4 -> "%esi"
            2 -> "%si"
            1 -> "%sil"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rdi(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rdi"
            4 -> "%edi"
            2 -> "%di"
            1 -> "%dil"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rdx(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rdx"
            4 -> "%edx"
            2 -> "%dx"
            1 -> "%dl"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rbp(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rbp"
            4 -> "%ebp"
            2 -> "%bp"
            1 -> "%bpl"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class R8(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r8"
            4 -> "%r8d"
            2 -> "%r8w"
            1 -> "%r8b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }    
}

class R9(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r9"
            4 -> "%r9d"
            2 -> "%r9w"
            1 -> "%r9b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class R10(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r10"
            4 -> "%r10d"
            2 -> "%r10w"
            1 -> "%r10b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class R11(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r11"
            4 -> "%r11d"
            2 -> "%r11w"
            1 -> "%r11b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class R12(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r12"
            4 -> "%r12d"
            2 -> "%r12w"
            1 -> "%r12b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class R13(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r13"
            4 -> "%r13d"
            2 -> "%r13w"
            1 -> "%r13b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class R14(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r14"
            4 -> "%r14d"
            2 -> "%r14w"
            1 -> "%r14b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }    
}

class R15(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%r15"
            4 -> "%r15d"
            2 -> "%r15w"
            1 -> "%r15b"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Rsp(override val size: Int): GPRegister {
    override fun toString(): String {
        val string = when (size) {
            8 -> "%rsp"
            4 -> "%esp"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Ymm0(override val size: Int): FPURegister {
    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm0"
            16 -> "%xmm0"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }    
}

class Ymm1(override val size: Int): FPURegister {
    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm1"
            16 -> "%xmm1"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Ymm2(override val size: Int): FPURegister {
    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm2"
            16 -> "%xmm2"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Ymm3(override val size: Int): FPURegister {
    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm3"
            16 -> "%xmm3"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Ymm4(override val size: Int): FPURegister {
    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm4"
            16 -> "%xmm4"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Ymm5(override val size: Int): FPURegister {
    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm5"
            16 -> "%xmm5"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }
}

class Mem(private val base: GPRegister, private val offset: Long, override val size: Int): Operand {
    override fun toString(): String {
        return if (offset == 0L) {
            "($base)"
        } else {
            "$offset($base)"
        }
    }
}

class Imm(val value: Long, override val size: Int): Operand {
    override fun toString(): String {
        return "$$value"
    }
}