package typedesc

import types.CompletedType


class VarDescriptor(val name: String, private val cType: CompletedType, private val qualifiers: List<TypeQualifier>, val storageClass: StorageClass?) {
    fun cType(): CompletedType {
        return cType
    }

    fun qualifiers(): List<TypeQualifier> {
        return qualifiers
    }

    fun toTypeDesc(): TypeDesc {
        return TypeDesc.from(cType, qualifiers)
    }

    override fun toString(): String = buildString {
        storageClass?.let {
            append(it)
            append(" ")
        }
        qualifiers.forEach {
            append(it)
            append(" ")
        }
        append(cType)
    }
}