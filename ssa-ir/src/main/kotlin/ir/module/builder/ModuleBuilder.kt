package ir.module.builder

import ir.*
import ir.module.SSAModule
import ir.module.Module
import ir.pass.ana.VerifySSA
import ir.types.PrimitiveType
import ir.types.Type

class ModuleBuilder {
    private val functions = arrayListOf<FunctionDataBuilder>()
    private val externFunctions = mutableSetOf<ExternFunction>()

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

    fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        return VerifySSA.run(SSAModule(fns, externFunctions))
    }

    companion object {
        fun create() : ModuleBuilder {
            return ModuleBuilder()
        }
    }
}