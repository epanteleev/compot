package types

import ir.Definitions.BYTE_SIZE
import typedesc.TypeDesc


class CFunctionType(val retType: TypeDesc, val argsTypes: List<TypeDesc>, var variadic: Boolean): CompletedType() {
    override fun size(): Int = BYTE_SIZE
    override fun alignmentOf(): Int = BYTE_SIZE

    override fun toString(): String = buildString {
        append(retType)
        append("(")
        argsTypes.forEachIndexed { index, type ->
            append(type)
            if (index < argsTypes.size - 1) append(", ")
        }
        if (variadic) append(", ...")
        append(")")
    }

    fun retType(): TypeDesc = retType
    fun args(): List<TypeDesc> = argsTypes
    fun variadic(): Boolean = variadic
    fun asPointer(): CPointer = CPointer(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CFunctionType) return false

        if (retType != other.retType) return false
        if (argsTypes != other.argsTypes) return false
        if (variadic != other.variadic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = retType.hashCode()
        result = 31 * result + argsTypes.hashCode()
        result = 31 * result + variadic.hashCode()
        return result
    }
}