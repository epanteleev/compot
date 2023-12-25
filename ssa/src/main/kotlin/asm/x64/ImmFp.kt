package asm.x64

import java.lang.IllegalArgumentException

sealed interface ImmFp : Imm {
    fun bits(): ImmInt

    operator fun plus(other: ImmFp): ImmFp
}

data class ImmFp32(val fp: Float): ImmFp {
    override fun toString(): String {
        return fp.toString()
    }

    override fun bits(): ImmInt {
        return ImmInt(fp.toBits().toLong())
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
        return ImmInt(fp.toBits())
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