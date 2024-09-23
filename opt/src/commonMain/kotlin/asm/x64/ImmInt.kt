package asm.x64

import asm.x64.ImmInt.Companion.canBeImm32
import common.assertion
import kotlin.jvm.JvmInline


sealed interface ImmInt: Imm {
    fun value(): Long

    fun asImm32(): Imm32

    companion object {
        const val minusZeroFloat: Long = 0x80000000
        val minusZeroDouble: Long = (-0.0).toBits()

        fun canBeImm32(constant: Long): Boolean {
            return Int.MIN_VALUE <= constant && constant <= Int.MAX_VALUE
        }
    }
}

@JvmInline
value class Imm32 private constructor(private val value: Int) : ImmInt {
    init {
        require(Int.MIN_VALUE <= value && value <= Int.MAX_VALUE) {
            "value=$value is not in range of Int"
        }
    }

    override fun toString(): String {
        return "$$value"
    }

    fun toInt(): Int = value

    override fun value(): Long = value.toLong()

    override fun toString(size: Int): String {
        return toString()
    }

    override fun asImm32(): Imm32 = this

    companion object {
        fun of(value: Long): Imm32 {
            assertion(canBeImm32(value)) {
                "cannot be cast to imm32: value=$value"
            }
            return Imm32(value.toInt())
        }

        fun of(value: Int): Imm32 {
            return Imm32(value)
        }
    }
}

@JvmInline
value class Imm64 private constructor(val value: Long) : ImmInt {
    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value

    override fun toString(size: Int): String {
        return toString()
    }

    override fun asImm32(): Imm32 {
        assertion(canBeImm32(value)) {
            "cannot be cast to imm32: value=$value"
        }

        return Imm32.of(value)
    }

    companion object {
        fun of(value: Long): Imm64 {
            return Imm64(value)
        }
    }
}