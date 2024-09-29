package types

class TypeDef(val name: String, val baseType: TypeDesc): CType() {
    fun baseType(): TypeDesc = baseType
    override fun typename(): String = name
    override fun size(): Int = baseType.size()
    override fun toString(): String = baseType.toString()
}