package typedesc

import types.CType

class VarDescriptor(val name: String, private val typeDesc: TypeDesc, val storageClass: StorageClass?) {
    fun cType(): CType {
        return typeDesc.cType()
    }

    fun qualifiers(): List<TypeQualifier> {
        return typeDesc.qualifiers()
    }

    override fun toString(): String {
        return if (storageClass == null) {
            typeDesc.toString()
        } else {
            "$storageClass $typeDesc"
        }
    }
}