package types

import tokenizer.Position
import typedesc.TypeDesc


sealed class AnyCArrayType(val type: TypeDesc): CAggregateType() {
    fun asPointer(): CPointer {
        return CPointer(type.cType())
    }

    fun element(): TypeDesc = type

    fun completedType(): CompletedType? = when (val cType = type.cType()) {
        is CompletedType -> cType
        else -> null
    }
}

class CStringLiteral(val dimension: Long): AnyCArrayType(TypeDesc.from(CHAR)) {
    override fun toString(): String = buildString {
        append(type)
        append("[$dimension]")
    }

    override fun size(): Int {
        return CHAR.size() * dimension.toInt()
    }

    override fun alignmentOf(): Int = CHAR.alignmentOf()

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
        return type.asType<CompletedType>(Position.UNKNOWN).size() * dimension.toInt() //TODO
    }

    override fun toString(): String = buildString {
        append("[$dimension]")
        append(type)
    }

    override fun alignmentOf(): Int {
        return type.asType<CompletedType>(Position.UNKNOWN).alignmentOf()
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

class CUncompletedArrayType(elementType: TypeDesc) : AnyCArrayType(elementType) {
    override fun size(): Int = 0
    override fun alignmentOf(): Int = 1

    override fun toString(): String = buildString {
        append("[]")
        append(type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CUncompletedArrayType) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}