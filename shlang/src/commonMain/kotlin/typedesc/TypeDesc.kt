package typedesc

import tokenizer.Position
import types.CType

data class TypeResolutionException(override val message: String, private val position: Position) : Exception(message)

class TypeDesc(private val baseType: CType, private val properties: List<TypeQualifier>) {
    fun qualifiers(): List<TypeQualifier> = properties
    fun cType(): CType = baseType
    fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return TypeDesc(baseType, properties + extraProperties)
    }

    inline fun<reified T: CType> asType(): T {
        val cTy = cType()
        if (cTy !is T) {
            throw TypeResolutionException("Type $cTy is not of type ${T::class.simpleName}", Position.UNKNOWN) // TODO unknown pos???
        }

        return cTy
    }

    override fun toString(): String = buildString {
        properties.forEach {
            append(it)
            append(" ")
        }
        append(baseType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeDesc) return false
        if (baseType != other.baseType) return false

        return true
    }

    override fun hashCode(): Int {
        return baseType.hashCode()
    }

    companion object {
        fun from(baseType: CType, properties: List<TypeQualifier> = arrayListOf()): TypeDesc = TypeDesc(baseType, properties)
    }
}