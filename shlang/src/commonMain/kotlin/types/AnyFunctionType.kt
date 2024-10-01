package types

import typedesc.TypeDesc


sealed interface AnyFunctionType {
    fun retType(): TypeDesc
    fun args(): List<TypeDesc>
    fun variadic(): Boolean
}

class AbstractCFunction(val retType: TypeDesc, val argsTypes: List<TypeDesc>, var variadic: Boolean): CAggregateType(), AnyFunctionType {
    override fun size(): Int = throw RuntimeException("Function type has no size")

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
}

class CFunctionType(val name: String, val functionType: AbstractCFunction): CAggregateType(), AnyFunctionType {
    override fun size(): Int = throw RuntimeException("Function type has no size")

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
}