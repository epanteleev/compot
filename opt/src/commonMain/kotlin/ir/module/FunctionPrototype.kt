package ir.module

import ir.types.Type
import ir.global.FunctionSymbol
import ir.types.NonTrivialType


sealed class AnyFunctionPrototype(protected val returnType: Type, protected val arguments: List<NonTrivialType>, val isVararg: Boolean) {
    fun arguments(): List<NonTrivialType> = arguments
    fun argument(index: Int): NonTrivialType {
        if (index < 0 || index >= arguments.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for arguments of size ${arguments.size}")
        }

        return arguments[index]
    }
    fun returnType(): Type = returnType
    abstract fun shortDescription(): String
}

sealed class DirectFunctionPrototype(val name: String, returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean = false):
    AnyFunctionPrototype(returnType, arguments, isVararg), FunctionSymbol {
    override fun shortDescription(): String {
        val builder = StringBuilder()
        builder.append("$returnType @$name(")
        arguments.joinTo(builder)
        builder.append(")")
        return builder.toString()
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
    final override fun type(): NonTrivialType = Type.Ptr
    final override fun name(): String = name
}

class FunctionPrototype(name: String, returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean = false):
   DirectFunctionPrototype(name, returnType, arguments, isVararg), FunctionSymbol {
    override fun toString(): String {
        return "define ${shortDescription()}"
    }
}

class IndirectFunctionPrototype(returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean):
    AnyFunctionPrototype(returnType, arguments, isVararg) {

    override fun shortDescription(): String {
        val builder = StringBuilder()
        builder.append("$returnType @<indirect>(")
        arguments.joinTo(builder)
        builder.append(")")
        return builder.toString()
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

class ExternFunction internal constructor(name: String, returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean):
    DirectFunctionPrototype(name, returnType, arguments, isVararg), FunctionSymbol {
    override fun toString(): String {
        return "extern ${shortDescription()}"
    }
}