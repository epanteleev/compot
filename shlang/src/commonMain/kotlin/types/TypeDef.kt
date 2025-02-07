package types

import typedesc.TypeDesc

class TypeDef(val name: String, private val baseType: TypeDesc): CType() {
    fun typeDesc(): TypeDesc = baseType
    fun cType(): CType = baseType.cType()
    override fun toString(): String = baseType.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeDef) return false

        if (name != other.name) return false
        if (baseType != other.baseType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + baseType.hashCode()
        return result
    }
}