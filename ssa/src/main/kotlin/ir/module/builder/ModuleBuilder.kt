package ir.module.builder

import ir.*
import ir.module.Module
import ir.module.SSAModule
import ir.pass.ana.VerifySSA
import ir.types.StructType
import ir.types.Type

class ModuleBuilder {
    private val functions = arrayListOf<FunctionDataBuilder>()
    private val externFunctions = mutableSetOf<ExternFunction>()
    private val globals = mutableSetOf<GlobalValue>()
    private val structs = arrayListOf<StructType>()

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

    fun addConstant(global: GlobalValue): GlobalValue {
        globals.add(global)
        return global
    }

    fun addStructType(structType: StructType): StructType {
        structs.add(structType)
        return structType
    }

    fun build(): Module {
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