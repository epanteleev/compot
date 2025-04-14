package asm.x64

import common.assertion
import common.getOrSet


sealed class Imm: Operand {
    abstract fun value(): Long

    abstract fun asImm32(): Imm32

    override fun toString(size: Int): String {
        return toString()
    }

    companion object {
        internal const val SIZE = 0xFL
        internal const val MASK = (SIZE - 1).inv()

        internal inline fun<reified T: Number, reified V: Imm> getOrCreate(i8: T, table: Array<V?>, fn: () -> V): V {
            val asInt = i8.toLong()
            if (asInt and MASK != 0L) {
                return fn()
            }

            return table.getOrSet(i8.toInt()) { fn() }
        }

        fun canBeImm32(constant: Long): Boolean {
            return Int.MIN_VALUE <= constant && constant <= Int.MAX_VALUE
        }

        fun canBeImm8(constant: Long): Boolean {
            return Byte.MIN_VALUE <= constant && constant <= Byte.MAX_VALUE
        }
    }
}

class Imm8 private constructor(private val value: Byte) : Imm() {
    init {
        require(Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
            "value=$value is not in range of Byte"
        }
    }

    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value.toLong()
    override fun asImm32(): Imm32 = Imm32.of(value.toInt())

    companion object {
        private val table = arrayOfNulls<Imm8>(SIZE.toInt())

        fun of(value: Long): Imm8 {
            if (!canBeImm8(value)) {
                throw IllegalArgumentException("value=$value is not in range of Byte")
            }

            return round(value)
        }

        fun round(value: Long): Imm8 {
            return getOrCreate(value and 0xFF, table) { Imm8(value.toByte()) }
        }
    }
}

class Imm32 private constructor(private val value: Int) : Imm() {
    init {
        require(Int.MIN_VALUE <= value && value <= Int.MAX_VALUE) {
            "value=$value is not in range of Int"
        }
    }

    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value.toLong()
    override fun asImm32(): Imm32 = this

    companion object {
        private val table = arrayOfNulls<Imm32>(SIZE.toInt())

        fun of(value: Long): Imm32 {
            if (!canBeImm32(value)) {
                throw IllegalArgumentException("value=$value is not in range of Int")
            }

            return getOrCreate(value, table) { Imm32(value.toInt()) }
        }

        fun of(value: Int): Imm32 {
            return getOrCreate(value, table) { Imm32(value) }
        }
    }
}

class Imm64 private constructor(val value: Long) : Imm() {
    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value

    override fun toString(size: Int): String {
        return toString()
    }

    override fun asImm32(): Imm32 {
        if (!canBeImm32(value)) {
            throw IllegalArgumentException("value=$value is not in range of Int")
        }

        return Imm32.of(value)
    }

    companion object {
        private val table = arrayOfNulls<Imm64>(SIZE.toInt())

        fun of(value: Int): Imm64 = getOrCreate(value, table) { Imm64(value.toLong()) }
        fun of(value: Long): Imm64 = getOrCreate(value, table) { Imm64(value) }
    }
}