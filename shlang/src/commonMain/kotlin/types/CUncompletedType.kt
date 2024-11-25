package types

import ir.Definitions.BYTE_SIZE
import typedesc.TypeDesc

sealed interface CUncompletedType

data class CUncompletedArrayType(val elementType: TypeDesc) : AnyCArrayType(elementType){
    override fun typename(): String {
        return toString()
    }

    override fun size(): Int = BYTE_SIZE

    override fun alignmentOf(): Int = BYTE_SIZE

    override fun toString(): String = buildString {
        append("[]")
        append(elementType)
    }
}

data class CUncompletedStructType(val name: String): CUncompletedType, CType() {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type '$name'")
    override fun alignmentOf(): Int = throw Exception("Uncompleted type '$this'")
    override fun toString(): String {
        return "struct $name"
    }
}

data class CUncompletedUnionType(val name: String): CUncompletedType, CType() {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type")
    override fun alignmentOf(): Int = throw Exception("Uncompleted type '$this'")

    override fun toString(): String {
        return "union $name"
    }
}

data class CUncompletedEnumType(val name: String): CUncompletedType, CPrimitive() {
    override fun typename(): String = "enum $name"

    override fun size(): Int = throw Exception("Uncompleted type") //TODO remove this
}