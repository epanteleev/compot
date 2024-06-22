package ir.read.bulder

import ir.module.*
import ir.module.builder.AnyModuleBuilder
import ir.types.StructType
import ir.pass.ana.VerifySSA
import ir.read.tokens.LocalValueToken
import ir.read.tokens.SymbolValue
import ir.read.tokens.TypeToken
import ir.types.NonTrivialType


class ModuleBuilderWithContext: TypeResolver, AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = hashMapOf<String, ExternFunction>()

    fun createFunction(functionName: SymbolValue, returnType: TypeToken, argumentTypes: List<TypeToken>, argumentValues: List<LocalValueToken>): FunctionDataBuilderWithContext {
        val args        = resolveArgumentType(argumentTypes)
        val prototype   = FunctionPrototype(functionName.name, returnType.type(this), args)

        val data = FunctionDataBuilderWithContext.create(this, prototype, argumentValues)
        functions.add(data)
        globals[functionName.name] = prototype
        return data
    }

    override fun resolve(name: String): StructType {
        return findStructType(name)
    }

    fun createExternFunction(functionName: SymbolValue, returnType: TypeToken, arguments: List<TypeToken>): ExternFunction {
        val resolvedArguments = resolveArgumentType(arguments)
        val extern = ExternFunction(functionName.name, returnType.type(this), resolvedArguments)
        externFunctions[functionName.name] = extern
        return extern
    }

    override fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        val ssa = SSAModule(fns, externFunctions, globals, structs)
        return VerifySSA.run(ssa)
    }

    internal fun resolveArgumentType(tokens: List<TypeToken>): List<NonTrivialType> {
        return tokens.mapTo(arrayListOf()) {
            val resolved = it.type(this)
            if (resolved !is NonTrivialType) {
                throw ParseErrorException("non-trivial type", it)
            }
            resolved
        }
    }

    companion object {
        fun create() : ModuleBuilderWithContext {
            return ModuleBuilderWithContext()
        }
    }
}