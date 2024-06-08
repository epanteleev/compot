package asm.x64

import asm.x64.ImmInt.Companion.canBeImm32
import common.assertion

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

data class Imm32(private val value: Long) : ImmInt {
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
}

data class Imm64(val value: Long) : ImmInt {
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

        return Imm32(value)
    }
}