package types

import typedesc.TypeDesc

class TypeDef(val name: String, private val baseType: TypeDesc): CType() {
    fun typeDesc(): TypeDesc = baseType
    fun cType(): CType = baseType.cType()
    override fun size(): Int = baseType.size()
    override fun toString(): String = baseType.toString()
    override fun alignmentOf(): Int = baseType.cType().alignmentOf()
}