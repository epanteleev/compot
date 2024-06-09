package asm.x64

import asm.x64.ImmInt.Companion.canBeImm32
import common.assertion


sealed interface ImmInt: Imm {
    fun value(): Long

    fun asImm32(): Imm32

    fun isScaleFactor(): Boolean = when (value()) {
        1L, 2L, 4L, 8L -> true
        else -> false
    }

    companion object {
        const val minusZeroFloat: Long = 0x80000000
        val minusZeroDouble: Long = (-0.0).toBits()

        fun canBeImm32(constant: Long): Boolean {
            return Int.MIN_VALUE <= constant && constant <= Int.MAX_VALUE
        }
    }
}

class Imm32 private constructor(private val value: Long) : ImmInt {
    init {
        require(Int.MIN_VALUE < value && value < Int.MAX_VALUE) //TODO
    }

    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value

    override fun toString(size: Int): String {
        return toString()
    }

    override fun asImm32(): Imm32 = this

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Imm32

        return value == other.value
    }

    companion object {
        fun of(value: Long): Imm32 {
            // TODO object pool???
            return Imm32(value)
        }
    }
}

class Imm64 private constructor(val value: Long) : ImmInt {
    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value

    override fun toString(size: Int): String {
        return toString()
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun asImm32(): Imm32 {
        assertion(canBeImm32(value)) {
            "cannot be cast to imm32: value=$value"
        }

        return Imm32.of(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Imm64

        return value == other.value
    }

    companion object {
        fun of(value: Long): Imm64 {
            // TODO object pool???
            return Imm64(value)
        }
    }
}