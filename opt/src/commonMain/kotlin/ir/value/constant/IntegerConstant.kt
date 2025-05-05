package ir.value.constant

import ir.types.*
import common.getOrSet


sealed interface IntegerConstant: PrimitiveConstant {
    fun toInt(): Int = when (this) {
        is UnsignedIntegerConstant -> value().toInt()
        is SignedIntegerConstant   -> value().toInt()
    }

    fun value(): Long

    infix fun or(other: IntegerConstant): IntegerConstant {
        val r = value() or other.value()
        return of(type().asType(),r)
    }

    infix fun and(other: IntegerConstant): IntegerConstant {
        val r = value() and other.value()
        return of(type().asType(), r)
    }

    infix fun xor(other: IntegerConstant): IntegerConstant {
        val r = value() xor other.value()
        return of(type().asType(), r)
    }

    infix fun shl(other: IntegerConstant): IntegerConstant {
        val r = value() shl other.value().toInt()
        return of(type().asType(), r)
    }

    companion object {
        internal const val SIZE = 0xFL
        internal const val MASK = (SIZE - 1).inv()

        internal inline fun<reified T: Number, reified V: IntegerConstant> getOrCreate(i8: T, table: Array<V?>, fn: () -> V): V {
            val asInt = i8.toLong()
            if (asInt and MASK != 0L) {
                return fn()
            }

            return table.getOrSet(i8.toInt()) { fn() }
        }

        fun of(kind: IntegerType, value: Number): IntegerConstant = when (kind) {
            is UnsignedIntType -> UnsignedIntegerConstant.of(kind, value)
            is SignedIntType -> SignedIntegerConstant.of(kind, value)
        }
    }
}