package typedesc

import types.CType

class VarDescriptor(val type: TypeDesc, val storageClass: StorageClass?) {
    fun cType(): CType {
        return type.cType()
    }

    override fun toString(): String {
        return if (storageClass == null) {
            type.toString()
        } else {
            "$storageClass $type"
        }
    }
}