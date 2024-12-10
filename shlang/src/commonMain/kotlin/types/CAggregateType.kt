package types

import ir.Definitions
import ir.Definitions.QWORD_SIZE
import typedesc.TypeDesc

// TODO MANY CODE DUPLICATES iN THIS FILE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
sealed class CAggregateType: CType() {
    fun hasFloatOnly(lo: Int, hi: Int): Boolean {
        return hasFloat(this, lo, hi, 0)
    }

    private fun hasFloat(ty: CType, lo: Int, hi: Int, offset: Int): Boolean {
        if (ty is AnyCStructType) {
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

sealed class AnyCStructType(open val name: String, protected val fields: List<Member>): CAggregateType() {
    override fun typename(): String = name

    abstract fun fieldByIndexOrNull(name: String): Int?

    fun fieldByIndexOrNull(index: Int): TypeDesc? {
        if (index < 0 || index >= fields.size) {
            return null
        }

        return fields[index].typeDesc()
    }

    fun fieldByIndex(index: Int): TypeDesc {
        return fieldByIndexOrNull(index) ?:
            throw RuntimeException("Cannon find field by index: index=$index, { name=$name, $fields }")
    }

    fun field(name: String): CType? {
        val field = fields.find { it is FieldMember && it.name == name }
        return field?.cType()
    }

    fun fields(): Collection<Member> {
        return fields
    }

    fun isSmall(): Boolean {
        return size() <= QWORD_SIZE * 2
    }

    abstract fun offset(index: Int): Int
}

class CStructType(override val name: String, fields: List<Member>): AnyCStructType(name, fields) {
    private val alignments = alignments()
    private var maxAlignment = Int.MIN_VALUE

    override fun alignmentOf(): Int {
        if (maxAlignment == Int.MIN_VALUE) {
            maxAlignment = alignments.maxOrNull() ?: 1
        }
        return maxAlignment
    }

    override fun fieldByIndexOrNull(name: String): Int? {
        var offset = 0
        for ((idx, field) in fields.withIndex()) {
            when (field) {
                is FieldMember -> {
                    if (field.name == name) {
                        return idx + offset
                    }
                }
                is AnonMember -> when (val cType = field.cType()) {
                    is CUnionType -> {
                        val i = cType.fieldByIndexOrNull(name)
                        if (i != null) {
                            return idx + offset
                        }
                        offset += cType.fields().size
                    }
                    is CStructType -> {
                        val i = cType.fieldByIndexOrNull(name)
                        if (i != null) {
                            return idx + i + offset
                        }
                        offset += cType.fields().size
                    }
                }
            }
        }
        return null
    }

    private fun alignments(): IntArray {
        var current = 0
        val result = IntArray(fields.size)
        for (i in fields.indices) {
            val field = fields[i]
            val alignment = field.cType().alignmentOf()
            current = Definitions.alignTo(current + field.cType().size(), alignment)
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
            offset = Definitions.alignTo(offset + fields[idx].size(), alignments[idx])
        }
        return Definitions.alignTo(offset, alignmentOf())
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

class CUnionType(override val name: String, fields: List<Member>): AnyCStructType(name, fields) {
    override fun fieldByIndexOrNull(name: String): Int? {
        if (fields.isEmpty()) {
            return null
        }
        return 0
    }

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

    override fun alignmentOf(): Int {
        return fields.maxOf { it.cType().size() }
    }
}

sealed class AnyCArrayType(val type: TypeDesc): CAggregateType() {
    fun asPointer(): CPointer {
        return CPointer(type.cType())
    }

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

    override fun alignmentOf(): Int {
        if (maxAlignment == Int.MIN_VALUE) {
            val cType = type.cType()
            maxAlignment = cType.alignmentOf()
        }
        return maxAlignment
    }
}