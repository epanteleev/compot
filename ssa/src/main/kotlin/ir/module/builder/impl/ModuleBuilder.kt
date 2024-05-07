package ir.module.builder.impl

import ir.*
import ir.module.AnyFunctionPrototype
import ir.module.ExternFunction
import ir.types.Type
import ir.module.Module
import ir.module.SSAModule
import ir.pass.ana.VerifySSA
import ir.module.builder.AnyModuleBuilder


class ModuleBuilder private constructor(): AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilder>()
    private val externFunctions = mutableSetOf<ExternFunction>()

    fun findFunction(name: String): AnyFunctionPrototype {
        val fnBuilder: AnyFunctionPrototype = functions.find { it.prototype().name() == name }?.prototype()
            ?: externFunctions.find { it.name() == name }
            ?: throw RuntimeException("not found name=$name") //TODO O(n)
        return fnBuilder
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<Type>): FunctionDataBuilder {
        val data = FunctionDataBuilder.create(name, returnType, argumentTypes)
        functions.add(data)
        return data
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<Type>, argumentValues: List<ArgumentValue>): FunctionDataBuilder {
        val data = FunctionDataBuilder.create(name, returnType, argumentTypes, argumentValues)
        functions.add(data)
        return data
    }

    fun createExternFunction(name: String, returnType: Type, arguments: List<Type>): ExternFunction {
        val extern = ExternFunction(name, returnType, arguments)
        externFunctions.add(extern)
        return extern
    }

    override fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        return VerifySSA.run(SSAModule(fns, externFunctions, globals, structs))
    }

    companion object {
        fun create() : ModuleBuilder {
            return ModuleBuilder()
        }
    }
}