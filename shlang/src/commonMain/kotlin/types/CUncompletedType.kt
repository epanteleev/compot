package types

import typedesc.TypeDesc

sealed interface CUncompletedType

data class CUncompletedArrayType(val elementType: TypeDesc) : AnyCArrayType(elementType){
    override fun typename(): String {
        return toString()
    }

    override fun size(): Int {
        return -1
    }

    override fun toString(): String = buildString {
        append("[]")
        append(elementType)
    }
}

data class CUncompletedStructType(override val name: String): CUncompletedType, AnyStructType(name) {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type '$name'")

    override fun toString(): String {
        return "struct $name"
    }
}

data class CUncompletedUnionType(override val name: String): CUncompletedType, AnyStructType(name) {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type")

    override fun toString(): String {
        return "union $name"
    }
}

data class CUncompletedEnumType(val name: String): CUncompletedType, CPrimitive() {
    override fun typename(): String = "enum $name"

    override fun size(): Int = throw Exception("Uncompleted type")
}