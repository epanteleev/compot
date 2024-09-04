package ir.types

import ir.Definitions.QWORD_SIZE


class StructType internal constructor(val name: String, val fields: List<NonTrivialType>): AggregateType {
    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = alignTo(current, fields[i].sizeOf()) + fields[i].sizeOf()
        }
        return alignTo(current, fields[index].sizeOf())
    }

    override fun field(index: Int): NonTrivialType {
        return fields[index]
    }

    override fun fields(): List<NonTrivialType> = fields

    override fun sizeOf(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        var offset = 0
        var alignment = 1
        for (field in fields) {
            alignment = if (field.sizeOf() <= QWORD_SIZE) {
                maxOf(alignment, field.sizeOf())
            } else {
                QWORD_SIZE
            }
            offset = alignTo(offset, alignment) + field.sizeOf()
        }
        return alignTo(offset, alignment)
    }

    override fun toString(): String = "$$name"

    fun hasFloatOnly(lo: Int, hi: Int): Boolean {
        return hasFloat(this, lo, hi, 0)
    }

    private fun hasFloat(ty: NonTrivialType, lo: Int, hi: Int, offset: Int): Boolean {
        if (ty is StructType) {
            for ((idx, field) in ty.fields.withIndex()) {
                if (!hasFloat(field, lo, hi, offset + ty.offset(idx))) { //TODO inefficient
                    return false
                }
            }
            return true

        } else if (ty is ArrayType) {
            for (i in 0 until ty.length) {
                if (!hasFloat(ty.elementType(), lo, hi, offset + i * ty.elementType().sizeOf())) {
                    return false
                }
            }
            return true
        }

        return offset < lo || hi <= offset || ty is FloatingPointType
    }

    fun dump(): String {
        return fields.joinToString(prefix = "$$name = type {", separator = ", ", postfix = "}")
    }

    companion object {
        private fun alignTo(value: Int, alignment: Int): Int {
            return ((value + alignment - 1) / alignment) * alignment
        }
    }
}