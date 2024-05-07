package ir.read.bulder

import ir.read.*
import ir.module.*
import ir.module.builder.AnyModuleBuilder
import ir.types.StructType
import ir.pass.ana.VerifySSA


class ModuleBuilderWithContext: TypeResolver, AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()

    fun createFunction(functionName: SymbolValue, returnType: TypeToken, argumentTypes: List<TypeToken>, argumentValues: List<LocalValueToken>): FunctionDataBuilderWithContext {
        val args        = argumentTypes.mapTo(arrayListOf()) { it.type(this) }
        val prototype   = FunctionPrototype(functionName.name, returnType.type(this), args)

        val data = FunctionDataBuilderWithContext.create(this, prototype, argumentValues)
        functions.add(data)
        globals.add(prototype)
        return data
    }

    override fun resolve(name: String): StructType {
        return findStructType(name)
    }

    fun createExternFunction(functionName: SymbolValue, returnType: TypeToken, arguments: List<TypeToken>): ExternFunction {
        val extern = ExternFunction(functionName.name, returnType.type(this), arguments.map { it.type(this) })
        externFunctions.add(extern)
        return extern
    }

    override fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        val ssa = SSAModule(fns, externFunctions, globals, structs)
        return VerifySSA.run(ssa)
    }

    companion object {
        fun create() : ModuleBuilderWithContext {
            return ModuleBuilderWithContext()
        }
    }
}