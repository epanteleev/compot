package types

import typedesc.TypeDesc


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