package ir.module.builder.impl

import ir.types.Type
import ir.module.Module
import ir.module.SSAModule
import ir.pass.ana.VerifySSA
import ir.value.ArgumentValue
import ir.module.AnyFunctionPrototype
import ir.module.builder.AnyModuleBuilder


class ModuleBuilder private constructor(): AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilder>()

    fun findFunction(name: String): AnyFunctionPrototype {
        val fnBuilder: AnyFunctionPrototype = functions.find { it.prototype().name() == name }?.prototype()
            ?: findExternFunctionOrNull(name) ?: throw RuntimeException("not found name=$name") //TODO O(n)
        return fnBuilder
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<Type>, isVararg: Boolean = false): FunctionDataBuilder {
        val data = FunctionDataBuilder.create(name, returnType, argumentTypes, isVararg)
        functions.add(data)
        return data
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<Type>, argumentValues: List<ArgumentValue>, isVararg: Boolean = false): FunctionDataBuilder {
        val data = FunctionDataBuilder.create(name, returnType, argumentTypes, argumentValues, isVararg)
        functions.add(data)
        return data
    }

    override fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        val module = SSAModule(fns, externFunctions, constantPool, globals, structs)
        return VerifySSA.run(module)
    }

    companion object {
        fun create() : ModuleBuilder {
            return ModuleBuilder()
        }
    }
}