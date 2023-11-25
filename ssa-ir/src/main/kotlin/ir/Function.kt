package ir

import ir.types.*

abstract class AnyFunctionPrototype(val name: String,
                                    private val returnType: Type,
                                    protected val arguments: List<Type>) {
    fun type(): Type {
        return returnType
    }

    fun arguments(): List<Type> {
        return arguments
    }


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
    AnyFunctionPrototype(name, returnType, arguments) {
    override fun toString(): String {
        return "define ${shortName()}"
    }
}

class ExternFunction internal constructor(name: String, returnType: Type, arguments: List<Type>):
    AnyFunctionPrototype(name, returnType, arguments) {
    override fun toString(): String {
        return "extern ${shortName()}"
    }
}