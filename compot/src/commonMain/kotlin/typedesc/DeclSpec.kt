package typedesc

class DeclSpec(val typeDesc: TypeDesc, val storageClass: StorageClass?) {
    override fun toString(): String = if (storageClass == null) {
        typeDesc.toString()
    } else {
        "$storageClass $typeDesc"
    }
}