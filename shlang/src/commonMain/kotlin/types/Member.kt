package types

import typedesc.TypeDesc

data class Member(val name: String, val typeDesc: TypeDesc) {
    fun cType(): CType {
        return typeDesc.cType()
    }

    fun size(): Int {
        return typeDesc.size()
    }
}