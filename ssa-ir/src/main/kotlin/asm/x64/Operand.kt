package asm.x64

import java.lang.RuntimeException

interface AnyOperand {
    val size: Int
}
interface Operand: AnyOperand

interface Register: Operand
interface FPURegister: Register {
    operator fun invoke(size: Int): FPURegister
}
interface GPRegister: Register {
    operator fun invoke(size: Int): GPRegister
}

enum class Rax(override val size: Int): GPRegister {
    rax(8),
    eax(4),
    ax(2),
    al(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rax.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rax {
            return when (size) {
                8 -> rax
                4 -> eax
                2 -> ax
                1 -> al
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rbx(override val size: Int): GPRegister {
    rbx(8),
    ebx(4),
    bx(2),
    bl(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rbx.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rbx {
            return when (size) {
                8 -> rbx
                4 -> ebx
                2 -> bx
                1 -> bl
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rcx(override val size: Int): GPRegister {
    rcx(8),
    ecx(4),
    cx(2),
    cl(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rcx.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rcx {
            return when (size) {
                8 -> rcx
                4 -> ecx
                2 -> cx
                1 -> cl
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rsi(override val size: Int): GPRegister {
    rsi(8),
    esi(4),
    si(2),
    sil(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rsi.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rsi {
            return when (size) {
                8 -> rsi
                4 -> esi
                2 -> si
                1 -> sil
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rdi(override val size: Int): GPRegister {
    rdi(8),
    edi(4),
    di(2),
    dil(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rdi.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rdi {
            return when (size) {
                8 -> rdi
                4 -> edi
                2 -> di
                1 -> dil
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rdx(override val size: Int): GPRegister {
    rdx(8),
    edx(4),
    dx(2),
    dl(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rdx.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rdx {
            return when (size) {
                8 -> rdx
                4 -> edx
                2 -> dx
                1 -> dl
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rbp(override val size: Int): GPRegister {
    rbp(8),
    ebp(4),
    bp(2),
    bpl(1);

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

    override fun invoke(size: Int): GPRegister {
        return Rbp.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rbp {
            return when (size) {
                8 -> rbp
                4 -> ebp
                2 -> bp
                1 -> bpl
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R8(override val size: Int): GPRegister {
    r8(8),
    r8d(4),
    r8w(2),
    r8b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R8.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R8 {
            return when (size) {
                8 -> r8
                4 -> r8d
                2 -> r8w
                1 -> r8b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R9(override val size: Int): GPRegister {
    r9(8),
    r9d(4),
    r9w(2),
    r9b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R9.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R9 {
            return when (size) {
                8 -> r9
                4 -> r9d
                2 -> r9w
                1 -> r9b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R10(override val size: Int): GPRegister {
    r10(8),
    r10d(4),
    r10w(2),
    r10b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R10.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R10 {
            return when (size) {
                8 -> r10
                4 -> r10d
                2 -> r10w
                1 -> r10b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R11(override val size: Int): GPRegister {
    r11(8),
    r11d(4),
    r11w(2),
    r11b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R11.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R11 {
            return when (size) {
                8 -> r11
                4 -> r11d
                2 -> r11w
                1 -> r11b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R12(override val size: Int): GPRegister {
    r12(8),
    r12d(4),
    r12w(2),
    r12b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R12.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R12 {
            return when (size) {
                8 -> r12
                4 -> r12d
                2 -> r12w
                1 -> r12b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R13(override val size: Int): GPRegister {
    r13(8),
    r13d(4),
    r13w(2),
    r13b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R13.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R13 {
            return when (size) {
                8 -> r13
                4 -> r13d
                2 -> r13w
                1 -> r13b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R14(override val size: Int): GPRegister {
    r14(8),
    r14d(4),
    r14w(2),
    r14b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R14.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R14 {
            return when (size) {
                8 -> r14
                4 -> r14d
                2 -> r14w
                1 -> r14b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class R15(override val size: Int): GPRegister {
    r15(8),
    r15d(4),
    r15w(2),
    r15b(1);

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

    override fun invoke(size: Int): GPRegister {
        return R15.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): R15 {
            return when (size) {
                8 -> r15
                4 -> r15d
                2 -> r15w
                1 -> r15b
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Rsp(override val size: Int): GPRegister {
    rsp(8),
    esp(4);

    override fun toString(): String {
        val string = when (size) {
            8 -> "%rsp"
            4 -> "%esp"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): GPRegister {
        return Rsp.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Rsp {
            return when (size) {
                8 -> rsp
                4 -> esp
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Ymm0(override val size: Int): FPURegister {
    ymm0(32),
    xmm0(16);

    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm0"
            16 -> "%xmm0"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): FPURegister {
        return Ymm0.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Ymm0 {
            return when (size) {
                32 -> ymm0
                16 -> xmm0
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Ymm1(override val size: Int): FPURegister {
    ymm1(32),
    xmm1(16);

    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm1"
            16 -> "%xmm1"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): FPURegister {
        return Ymm1.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Ymm1 {
            return when (size) {
                32 -> ymm1
                16 -> xmm1
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Ymm2(override val size: Int): FPURegister {
    ymm2(32),
    xmm2(16);

    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm2"
            16 -> "%xmm2"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): FPURegister {
        return Ymm2.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Ymm2 {
            return when (size) {
                32 -> ymm2
                16 -> xmm2
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Ymm3(override val size: Int): FPURegister {
    ymm3(32),
    xmm3(16);

    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm3"
            16 -> "%xmm3"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): FPURegister {
        return Ymm3.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Ymm3 {
            return when (size) {
                32 -> ymm3
                16 -> xmm3
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Ymm4(override val size: Int): FPURegister {
    ymm4(32),
    xmm4(16);

    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm4"
            16 -> "%xmm4"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): FPURegister {
        return Ymm4.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Ymm4 {
            return when (size) {
                32 -> ymm4
                16 -> xmm4
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

enum class Ymm5(override val size: Int): FPURegister {
    ymm5(32),
    xmm5(16);

    override fun toString(): String {
        val string = when (size) {
            32 -> "%ymm5"
            16 -> "%xmm5"
            else -> throw RuntimeException("Internal error")
        }
        return string
    }

    override fun invoke(size: Int): FPURegister {
        return Ymm5.invoke(size)
    }

    companion object {
        operator fun invoke(size: Int): Ymm5 {
            return when (size) {
                32 -> ymm5
                16 -> xmm5
                else -> throw IllegalArgumentException("Size $size not allowed.")
            }
        }
    }
}

open class Mem(protected open val base: GPRegister, open val offset: Long, override val size: Int): Operand {
    override fun toString(): String {
        return if (offset == 0L) {
            "($base)"
        } else {
            "$offset($base)"
        }
    }

    override fun hashCode(): Int {
        return offset.toInt() + size
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mem

        if (base != other.base) return false
        if (offset != other.offset) return false
        return size == other.size
    }
}

class ArgumentSlot(override val base: GPRegister, override var offset: Long, override val size: Int): Mem(base, offset, size) {
    fun updateOffset(newOffset: Long) {
        offset = newOffset
    }
}

class Imm(val value: Long, override val size: Int): AnyOperand {
    override fun toString(): String {
        return "$$value"
    }
}