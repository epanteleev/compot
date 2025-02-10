package ir.module

import ir.types.*
import ir.attributes.ByValue
import ir.attributes.FunctionAttribute
import ir.global.FunctionSymbol


sealed class AnyFunctionPrototype(protected val returnType: Type, protected val arguments: List<NonTrivialType>, val attributes: Set<FunctionAttribute>) {
    fun arguments(): List<NonTrivialType> = arguments
    fun argument(index: Int): NonTrivialType? {
        if (index < 0 || index >= arguments.size) {
            return null
        }

        return arguments[index]
    }
    fun returnType(): Type = returnType

    fun byValue(idx: Int): ByValue? {
        return attributes.find { it is ByValue && it.argumentIndex == idx } as ByValue?
    }

    abstract fun shortDescription(): String
}

sealed class DirectFunctionPrototype(val name: String, returnType: Type, arguments: List<NonTrivialType>, attributes: Set<FunctionAttribute>):
    AnyFunctionPrototype(returnType, arguments, attributes), FunctionSymbol {
    override fun shortDescription(): String = buildString {
        append("$returnType @$name(")
        for (i in arguments.indices) {
            append(arguments[i])
            if (i != arguments.size - 1) {
                append(", ")
            }
        }

        append(")")
        attributes.forEach {
            append(" $it")
        }
        return toString()
    }

    final override fun hashCode(): Int {
        return name.hashCode() + arguments.hashCode() + returnType.hashCode()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectFunctionPrototype) return false

        if (name != other.name) return false
        if (returnType != other.returnType) return false

        return true
    }

    final override fun dump(): String = toString()
    final override fun type(): NonTrivialType = PtrType
    final override fun name(): String = name
}

class FunctionPrototype(name: String, returnType: Type, arguments: List<NonTrivialType>, attributes: Set<FunctionAttribute>):
   DirectFunctionPrototype(name, returnType, arguments, attributes), FunctionSymbol {
    override fun toString(): String {
        return "define ${shortDescription()}"
    }
}

class IndirectFunctionPrototype(returnType: Type, arguments: List<NonTrivialType>, attributes: Set<FunctionAttribute>):
    AnyFunctionPrototype(returnType, arguments, attributes) {

    override fun shortDescription(): String = buildString {
        append("$returnType @<indirect>(")
        for (i in arguments.indices) {
            append(arguments[i])
            if (i != arguments.size - 1) {
                append(", ")
            }
        }
        append(")")
        attributes.forEach {
            append(" $it")
        }
        return toString()
    }

    override fun toString(): String {
        return "define ${shortDescription()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndirectFunctionPrototype) return false

        if (returnType() != other.returnType()) return false
        if (arguments() != other.arguments()) return false

        return true
    }

    override fun hashCode(): Int {
        return returnType().hashCode() + arguments().hashCode()
    }
}

class ExternFunction internal constructor(name: String, returnType: Type, arguments: List<NonTrivialType>, attributes: Set<FunctionAttribute>):
    DirectFunctionPrototype(name, returnType, arguments, attributes), FunctionSymbol {
    override fun toString(): String = buildString {
        append("extern ")
        append(shortDescription())
        attributes.forEach {
            append(" $it")
        }
        return toString()
    }
}