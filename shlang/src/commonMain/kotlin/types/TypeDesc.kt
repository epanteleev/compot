package types


data class TypeInferenceException(override val message: String) : Exception(message)

data class TypeResolutionException(override val message: String) : Exception(message)

class TypeDesc(private val baseType: CType, private val properties: List<TypeQualifier>) {
    fun qualifiers(): List<TypeQualifier> = properties
    fun baseType(): CType = baseType
    fun size(): Int = baseType.size()
    fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return TypeDesc(baseType, properties + extraProperties)
    }

    inline fun<reified T> asType(): T {
        return baseType() as T
    }

    override fun toString(): String = buildString {
        properties.forEach {
            append(it)
            append(" ")
        }
        append(baseType)
    }

    companion object {
        fun from(baseType: CType, properties: List<TypeQualifier> = arrayListOf()): TypeDesc = TypeDesc(baseType, properties)
    }
}