package types

import ir.Definitions
import typedesc.TypeDesc


sealed class CAggregateType: CType() {
    fun hasFloatOnly(lo: Int, hi: Int): Boolean {
        return hasFloat(this, lo, hi, 0)
    }

    private fun hasFloat(ty: CType, lo: Int, hi: Int, offset: Int): Boolean {
        if (ty is AnyStructType) {
            for ((idx, field) in ty.fields().withIndex()) {
                if (!hasFloat(field.cType(), lo, hi, offset + ty.offset(idx))) { //TODO inefficient
                    return false
                }
            }
            return true

        } else if (ty is CArrayType) {
            for (i in 0 until ty.dimension.toInt()) {
                if (!hasFloat(ty.type.cType(), lo, hi, offset + i * ty.type.cType().size())) {
                    return false
                }
            }
            return true
        }

        return offset < lo || hi <= offset || ty is FLOAT || ty is DOUBLE
    }
}

sealed class AnyStructType(open val name: String, protected val fields: List<Member>): CAggregateType() {
    override fun typename(): String = name

    fun fieldIndex(name: String): Int? {
        for ((idx, field) in fields.withIndex()) {
            when (field) {
                is FieldMember -> {
                    if (field.name == name) {
                        return idx
                    }
                }
                is AnonMember -> {
                    val i = field.cType().fieldIndex(name)
                    if (i != null) {
                        return idx + i
                    }
                }
            }
        }
        return null
    }

    fun fieldIndex(index: Int): TypeDesc? {
        if (index < 0 || index >= fields.size) {
            return null
        }
        return fields[index].typeDesc()
    }

    fun fields(): List<Member> {
        return fields
    }

    abstract fun offset(index: Int): Int
    abstract fun maxAlignment(): Int
}

class CStructType(override val name: String, fields: List<Member>): AnyStructType(name, fields) {
    private val alignments = alignments()
    private var maxAlignment = Int.MIN_VALUE

    override fun maxAlignment(): Int {
        if (maxAlignment == Int.MIN_VALUE) {
            maxAlignment = alignments.maxOrNull() ?: 1
        }
        return maxAlignment
    }

    private fun alignments(): IntArray {
        var current = 0
        var alignment = 1
        val result = IntArray(fields.size)
        for (i in fields.indices) {
            val field = fields[i]
            alignment = align(alignment, field.cType())
            current = Definitions.alignTo(current + field.size(), alignment)
            result[i] = alignment
        }
        return result
    }

    override fun size(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        var offset = 0
        for (idx in fields.indices) {
            offset = Definitions.alignTo(offset + fields[idx].typeDesc().size(), alignments[idx])
        }
        return offset
    }

    private fun align(alignment: Int, field: CType): Int = when (field) {
        is AnyStructType -> maxOf(alignment, field.maxAlignment())
        is CArrayType    -> maxOf(alignment, field.maxAlignment())
        else -> maxOf(alignment, field.size())
    }

    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = Definitions.alignTo(current + fields[i].size(), alignments[i])
        }
        return Definitions.alignTo(current, alignments[index])
    }

    override fun toString(): String = buildString {
        append("struct $name")
        append(" {")
        fields.joinTo(this, separator = "") { field -> field.toString() }
        append("}")
    }
}

class CUnionType(override val name: String, fields: List<Member>): AnyStructType(name, fields) {
    override fun size(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        return fields.maxOf { it.cType().size() }
    }

    override fun toString(): String = buildString {
        append("union $name")
        append(" {")
        fields.joinTo(this, separator = "") { field -> field.toString() }
        append("}")
    }

    override fun offset(index: Int): Int {
        return 0
    }

    override fun maxAlignment(): Int {
        return fields.maxOf { it.cType().size() }
    }
}

sealed class AnyCArrayType(val type: TypeDesc): CAggregateType() {
    fun element(): TypeDesc = type
}

class CArrayType(type: TypeDesc, val dimension: Long) : AnyCArrayType(type) {
    private var maxAlignment = Int.MIN_VALUE

    override fun typename(): String {
        return toString()
    }

    override fun size(): Int {
        return type.size() * dimension.toInt() //TODO
    }

    override fun toString(): String = buildString {
        append("[$dimension]")
        append(type)
    }

    fun offset(index: Int): Int {
        return index * type.size()
    }

    fun maxAlignment(): Int {
        if (maxAlignment == Int.MIN_VALUE) {
            val cType = type.cType()
            maxAlignment = when (cType) {
                is CArrayType    -> cType.maxAlignment()
                is AnyStructType -> cType.maxAlignment()
                else -> cType.size()
            }
        }
        return maxAlignment
    }
}