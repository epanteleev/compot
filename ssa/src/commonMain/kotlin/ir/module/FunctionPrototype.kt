package ir.module

import ir.types.Type
import ir.global.FunctionSymbol
import ir.read.tokens.Vararg
import ir.types.NonTrivialType

abstract class AnyFunctionPrototype(val name: String,
                                    private val returnType: Type,
                                    protected val arguments: List<Type>, val isVararg: Boolean) {

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

        return true
    }
}

class FunctionPrototype(name: String, returnType: Type, arguments: List<Type>, isVararg: Boolean = false):
    AnyFunctionPrototype(name, returnType, arguments, isVararg), FunctionSymbol {
    override fun toString(): String {
        return "define ${shortName()}"
    }

    override fun dump(): String = toString()
    override fun type(): NonTrivialType = Type.Ptr
    override fun name(): String = name
}

class IndirectFunctionPrototype(returnType: Type, arguments: List<Type>, isVararg: Boolean):
    AnyFunctionPrototype("<indirect>", returnType, arguments, isVararg) {
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

class ExternFunction internal constructor(name: String, returnType: Type, arguments: List<Type>, isVararg: Boolean):
    AnyFunctionPrototype(name, returnType, arguments, isVararg), FunctionSymbol {
    override fun toString(): String {
        return "extern ${shortName()}"
    }

    override fun type(): NonTrivialType = Type.Ptr
    override fun name(): String = name
    override fun dump(): String = toString()
}