package asm.x64

import common.assertion
import common.getOrSet


sealed class ImmInt: Imm {
    abstract fun value(): Long

    abstract fun asImm32(): Imm32

    override fun toString(size: Int): String {
        return toString()
    }

    companion object {
        internal const val SIZE = 0xFL
        internal const val MASK = (SIZE - 1).inv()

        internal inline fun<reified T: Number, reified V: ImmInt> getOrCreate(i8: T, table: Array<V?>, fn: () -> V): V {
            val asInt = i8.toLong()
            if (asInt and MASK != 0L) {
                return fn()
            }

            return table.getOrSet(i8.toInt()) { fn() }
        }

        fun canBeImm32(constant: Long): Boolean {
            return Int.MIN_VALUE <= constant && constant <= Int.MAX_VALUE
        }
    }
}

class Imm32 private constructor(private val value: Int) : ImmInt() {
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
            assertion(canBeImm32(value)) {
                "cannot be cast to imm32: value=$value"
            }

            return getOrCreate(value, table) { Imm32(value.toInt()) }
        }

        fun of(value: Int): Imm32 {
            assertion(canBeImm32(value.toLong())) {
                "cannot be cast to imm32: value=$value"
            }

            return getOrCreate(value, table) { Imm32(value) }
        }
    }
}

class Imm64 private constructor(val value: Long) : ImmInt() {
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
        private val table = arrayOfNulls<Imm64>(SIZE.toInt())

        fun of(value: Int): Imm64 = getOrCreate(value, table) { Imm64(value.toLong()) }
        fun of(value: Long): Imm64 = getOrCreate(value, table) { Imm64(value) }
    }
}