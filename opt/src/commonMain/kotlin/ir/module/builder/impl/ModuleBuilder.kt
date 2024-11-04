package ir.module.builder.impl

import ir.attributes.FunctionAttribute
import ir.types.Type
import ir.module.Module
import ir.module.SSAModule
import ir.pass.analysis.VerifySSA
import ir.value.ArgumentValue
import ir.module.AnyFunctionPrototype
import ir.module.DirectFunctionPrototype
import ir.module.builder.AnyModuleBuilder
import ir.types.NonTrivialType
import ir.types.PrimitiveType


class ModuleBuilder private constructor(): AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilder>()

    fun findFunction(name: String): DirectFunctionPrototype? {
        return functions.find { it.prototype().name() == name }?.prototype() ?: findExternFunctionOrNull(name)
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<NonTrivialType>, attributes: Set<FunctionAttribute> = hashSetOf()): FunctionDataBuilder {
        val data = FunctionDataBuilder.create(name, returnType, argumentTypes, attributes)
        functions.add(data)
        return data
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<NonTrivialType>, argumentValues: List<ArgumentValue>, attributes: Set<FunctionAttribute> = hashSetOf()): FunctionDataBuilder {
        val data = FunctionDataBuilder.create(name, returnType, argumentTypes, argumentValues, attributes)
        functions.add(data)
        return data
    }

    override fun build(): Module {
        val fns = functions.map { it.build() }.associateBy { it.name() }

        val module = SSAModule(fns, externFunctions, constantPool, globals, structs)
        return VerifySSA.run(module)
    }

    companion object {
        fun create() : ModuleBuilder {
            return ModuleBuilder()
        }
    }
}