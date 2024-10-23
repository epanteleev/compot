package typedesc

class VarDescriptor(val type: TypeDesc, val storageClass: StorageClass?) {
    override fun toString(): String {
        return if (storageClass == null) {
            type.toString()
        } else {
            "$storageClass $type"
        }
    }
}