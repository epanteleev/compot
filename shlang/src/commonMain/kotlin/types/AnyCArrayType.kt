package types

import ir.Definitions.BYTE_SIZE
import typedesc.TypeDesc


sealed class AnyCArrayType(val type: TypeDesc): CAggregateType() {
    fun asPointer(): CPointer {
        return CPointer(type.cType())
    }

    fun element(): TypeDesc = type
}

class CStringLiteral(elementType: TypeDesc, val dimension: Long): AnyCArrayType(elementType) {
    override fun toString(): String = buildString {
        append(type)
        append("[$dimension]")
    }

    override fun size(): Int {
        return type.size() * dimension.toInt()
    }

    override fun alignmentOf(): Int = type.cType().alignmentOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CStringLiteral) return false

        if (type != other.type) return false
        if (dimension != other.dimension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + dimension.hashCode()
        return result
    }
}

class CArrayType(type: TypeDesc, val dimension: Long) : AnyCArrayType(type) {
    private var maxAlignment = Int.MIN_VALUE

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CArrayType) return false

        if (type != other.type) return false
        if (dimension != other.dimension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + dimension.hashCode()
        return result
    }
}

data class CUncompletedArrayType(val elementType: TypeDesc) : AnyCArrayType(elementType) {
    override fun size(): Int = BYTE_SIZE
    override fun alignmentOf(): Int = BYTE_SIZE

    override fun toString(): String = buildString {
        append("[]")
        append(elementType)
    }
}