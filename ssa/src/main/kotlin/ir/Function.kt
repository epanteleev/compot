package ir

import ir.types.Type


abstract class AnyFunctionPrototype(val name: String,
                                    private val returnType: Type,
                                    protected val arguments: List<Type>) {

    fun arguments(): List<Type> = arguments

    fun returnType(): Type = returnType

    fun shortName(): String {
        val builder = StringBuilder()
        builder.append("$returnType @$name(")
        arguments.joinTo(builder)
        builder.append(")")
        return builder.toString()
    }

    override fun hashCode(): Int {
        return name.hashCode() + arguments.hashCode() + returnType.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyFunctionPrototype) return false

        if (name != other.name) return false
        if (returnType != other.returnType) return false
        if (arguments != other.arguments) return false

        return true
    }
}

class FunctionPrototype(name: String, returnType: Type, arguments: List<Type>):
    AnyFunctionPrototype(name, returnType, arguments), GlobalSymbol {
    override fun toString(): String {
        return "define ${shortName()}"
    }

    override fun dump(): String = toString()
    override fun name(): String = name
}

class IndirectFunctionPrototype(returnType: Type, arguments: List<Type>):
    AnyFunctionPrototype("<indirect>", returnType, arguments) {
    override fun toString(): String {
        return "define ${shortName()}"
    }

    override fun equals(other: Any?): Boolean {
        return this === other //TODO???
    }

    override fun hashCode(): Int {
        return returnType().hashCode() + arguments().hashCode()
    }
}

class ExternFunction internal constructor(name: String, returnType: Type, arguments: List<Type>):
    AnyFunctionPrototype(name, returnType, arguments), GlobalSymbol {
    override fun toString(): String {
        return "extern ${shortName()}"
    }

    override fun name(): String = name
    override fun dump(): String = toString()
}