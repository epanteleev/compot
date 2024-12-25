package typedesc

import types.CType

class VarDescriptor(val typeDesc: TypeDesc, val storageClass: StorageClass?) {
    fun cType(): CType {
        return typeDesc.cType()
    }

    override fun toString(): String {
        return if (storageClass == null) {
            typeDesc.toString()
        } else {
            "$storageClass $typeDesc"
        }
    }
}