package ir.value.constant

import ir.types.*
import common.getOrSet


sealed interface IntegerConstant: PrimitiveConstant {
    fun toInt(): Int = when (this) {
        is UnsignedIntegerConstant -> value().toInt()
        is SignedIntegerConstant   -> value().toInt()
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