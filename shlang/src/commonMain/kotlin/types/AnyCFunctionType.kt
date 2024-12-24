package types

import typedesc.TypeDesc


sealed class AnyCFunctionType: CType() {
    abstract fun retType(): TypeDesc
    abstract fun args(): List<TypeDesc>
    abstract fun variadic(): Boolean
}

class AbstractCFunction(val retType: TypeDesc, val argsTypes: List<TypeDesc>, var variadic: Boolean): AnyCFunctionType() {
    override fun size(): Int = throw RuntimeException("Function type has no size")
    override fun alignmentOf(): Int = throw RuntimeException("Function type has no alignment")

    override fun typename(): String = buildString {
        append(retType)
        append("(")
        argsTypes.forEachIndexed { index, type ->
            append(type)
            if (index < argsTypes.size - 1) append(", ")
        }
        if (variadic) append(", ...")
        append(")")
    }

    override fun retType(): TypeDesc {
        return retType
    }

    override fun args(): List<TypeDesc> {
        return argsTypes
    }

    override fun variadic(): Boolean {
        return variadic
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractCFunction) return false

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

class CFunctionType(val name: String, val functionType: AbstractCFunction): AnyCFunctionType() {
    override fun size(): Int = throw RuntimeException("Function type has no size")
    override fun alignmentOf(): Int = throw RuntimeException("Function type has no alignment")

    override fun retType(): TypeDesc = functionType.retType
    override fun args(): List<TypeDesc> = functionType.argsTypes

    override fun variadic(): Boolean {
        return functionType.variadic
    }

    override fun typename(): String = buildString {
        append(functionType.retType)
        append(" $name(")
        functionType.argsTypes.forEachIndexed { index, type ->
            append(type)
            if (index < functionType.argsTypes.size - 1) append(", ")
        }
        if (functionType.variadic) append(", ...")
        append(")")
    }

    fun asPointer(): CPointer = CPointer(this)
}