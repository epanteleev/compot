package ir.value.constant

import common.arrayFrom
import ir.types.TupleType


class TupleConstant(private val tuple: Array<PrimitiveConstant>): TrivialConstant {
    override fun type(): TupleType = TupleType(arrayFrom(tuple) { it.type() })

    override fun toString(): String {
        return tuple.joinToString(", ", "|", "|")
    }

    fun inner(idx: Int): PrimitiveConstant {
        if (idx < 0 || idx >= tuple.size) {
            throw IndexOutOfBoundsException("index=$idx, tuple=${tuple.joinToString { it.toString() }}")
        }

        return tuple[idx]
    }

    companion object {
        fun of(lhs: PrimitiveConstant, rhs: PrimitiveConstant): TupleConstant {
            return TupleConstant(arrayOf(lhs, rhs))
        }
    }
}