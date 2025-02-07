package types

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
        return type.asType<SizedType>().size() * dimension.toInt()
    }

    override fun alignmentOf(): Int = type.asType<SizedType>().alignmentOf()

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
    override fun size(): Int {
        return type.asType<SizedType>().size() * dimension.toInt() //TODO
    }

    override fun toString(): String = buildString {
        append("[$dimension]")
        append(type)
    }

    override fun alignmentOf(): Int {
        return type.asType<SizedType>().alignmentOf()
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
    override fun size(): Int = 0
    override fun alignmentOf(): Int = 1

    override fun toString(): String = buildString {
        append("[]")
        append(elementType)
    }
}