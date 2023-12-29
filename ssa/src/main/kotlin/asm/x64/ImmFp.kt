package asm.x64

import java.lang.IllegalArgumentException

sealed interface ImmFp : Imm {
    fun bits(): ImmInt

    operator fun unaryMinus(): ImmFp

    operator fun plus(other: ImmFp): ImmFp
}

data class ImmFp32(val fp: Float): ImmFp {
    override fun toString(): String {
        return fp.toString()
    }

    override fun bits(): ImmInt {
        return Imm64(fp.toBits().toLong())
    }

    override operator fun unaryMinus(): ImmFp {
        return ImmFp32(-fp)
    }

    override fun plus(other: ImmFp): ImmFp {
        return when (other) {
            is ImmFp64 -> ImmFp64(other.fp + fp)
            is ImmFp32 -> ImmFp32(other.fp + fp)
        }
    }

    override fun toString(size: Int): String {
        return toString()
    }
}

data class ImmFp64(val fp: Double): ImmFp {
    override fun toString(): String {
        return fp.toString()
    }

    override fun bits(): ImmInt {
        return Imm64(fp.toBits())
    }

    override operator fun unaryMinus(): ImmFp {
        return ImmFp64(-fp)
    }

    override fun plus(other: ImmFp): ImmFp {
        return when (other) {
            is ImmFp64 -> ImmFp64(other.fp + fp)
            is ImmFp32 -> ImmFp64(other.fp + fp)
        }
    }

    override fun toString(size: Int): String {
        return toString()
    }
}