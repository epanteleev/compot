package types

import typedesc.*
import ir.Definitions.POINTER_SIZE


class CPointer(val type: CType, private val properties: Set<TypeQualifier> = setOf()) : CPrimitive() {
    override fun size(): Int = POINTER_SIZE

    fun dereference(typeHolder: TypeHolder): CType= when (type) {
        is CFunctionType          -> type.functionType
        is CUncompletedStructType -> typeHolder.getStructType<CStructType>(type.name)
        is CUncompletedUnionType  -> typeHolder.getStructType<CUnionType>(type.name)
        is CUncompletedEnumType   -> typeHolder.getStructType<CEnumType>(type.name)
        else -> type
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CPointer) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String = buildString {
        properties.forEach { append(it) }
        append(type)
        append("*")
    }
}