package ir.read.bulder

import ir.*
import ir.builder.FunctionDataBuilder
import ir.builder.ModuleBuilder
import ir.pass.ana.VerifySSA
import ir.read.Identifier
import ir.read.TypeToken
import ir.read.ValueToken

class ModuleBuilderWithContext {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()

    fun createFunction(name: Identifier, returnType: TypeToken, argumentTypes: List<TypeToken>, argumentValues: List<ValueToken>): FunctionDataBuilderWithContext {
        val data = FunctionDataBuilderWithContext.create(name, returnType, argumentTypes, argumentValues)
        functions.add(data)
        return data
    }

    fun createExternFunction(name: String, returnType: Type, arguments: List<Type>): ExternFunction {
        val extern = ExternFunction(name, returnType, arguments)
        externFunctions.add(extern)
        return extern
    }

    fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        return VerifySSA.run(Module(fns, externFunctions))
    }

    companion object {
        fun create() : ModuleBuilder {
            return ModuleBuilder()
        }
    }
}