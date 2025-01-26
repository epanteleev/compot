package ir.read.bulder

import ir.attributes.VarArgAttribute
import ir.global.GlobalSymbol
import ir.module.*
import ir.read.tokens.*
import ir.module.builder.AnyModuleBuilder
import ir.types.StructType
import ir.pass.analysis.VerifySSA
import ir.types.NonTrivialType


class ModuleBuilderWithContext private constructor(): TypeResolver, AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()

    fun createFunction(functionName: SymbolValue, returnType: TypeToken, argumentTypes: List<TypeToken>, argumentValues: List<LocalValueToken>): FunctionDataBuilderWithContext {
        val args = resolveArgumentType(argumentTypes)
        val attributes = if (argumentTypes.lastOrNull() is Vararg) {
            hashSetOf(VarArgAttribute)
        } else {
            emptySet()
        }
        val prototype = FunctionPrototype(functionName.name, returnType.type(this), args, attributes)
        val data = FunctionDataBuilderWithContext.create(this, prototype, argumentValues)
        functions.add(data)
        return data
    }

    override fun resolve(name: String): StructType {
        return findStructTypeOrNull(name) ?: throw IllegalStateException("Struct type '$name' not found")
    }

    fun createExternFunction(functionName: SymbolValue, returnType: TypeToken, arguments: List<TypeToken>): ExternFunction {
        val resolvedArguments = resolveArgumentType(arguments)
        val attributes = if (arguments.lastOrNull() is Vararg) {
            hashSetOf(VarArgAttribute)
        } else {
            emptySet()
        }
        val extern = ExternFunction(functionName.name, returnType.type(this), resolvedArguments, attributes)
        functionDeclarations[functionName.name] = extern
        return extern
    }

    override fun build(): Module {
        val fns = functions.map { it.build() }.associateBy { it.name() }

        val ssa = SSAModule(fns, functionDeclarations, constantPool, globals, structs)
        return VerifySSA.run(ssa)
    }

    internal fun resolveArgumentType(tokens: List<TypeToken>): List<NonTrivialType> {
        fun convert(typeToken: TypeToken): NonTrivialType {
            val type = typeToken.type(this)
            if (type !is NonTrivialType) {
                throw IllegalStateException("Expected non-trivial type, but got '$type'")
            }

            return type
        }

        if (tokens.isEmpty()) {
            return arrayListOf()
        }
        return if (tokens.last() is Vararg) {
            tokens.dropLast(1).mapTo(arrayListOf()) { convert(it) }
        } else {
            tokens.mapTo(arrayListOf()) { convert(it) }
        }
    }

    fun findFunctionOrNull(name: String): GlobalSymbol? {
        val externFunction = functionDeclarations[name]
        if (externFunction != null) {
            return externFunction
        }

        val function = functions.find { it.prototype().name() == name }
        if (function != null) {
            return function.prototype()
        }
        return null
    }

    companion object {
        fun create() : ModuleBuilderWithContext {
            return ModuleBuilderWithContext()
        }
    }
}