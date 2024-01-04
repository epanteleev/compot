package ir.read.bulder

import ir.*
import ir.read.*
import ir.module.*
import ir.pass.ana.VerifySSA
import ir.types.StructType


class ModuleBuilderWithContext {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()
    private val globals = mutableSetOf<GlobalValue>()
    private val structs = arrayListOf<StructType>()

    fun createFunction(name: SymbolValue, returnType: ElementaryTypeToken, argumentTypes: List<ElementaryTypeToken>, argumentValues: List<LocalValueToken>): FunctionDataBuilderWithContext {
        val data = FunctionDataBuilderWithContext.create(name, returnType, argumentTypes, argumentValues, globals)
        functions.add(data)
        return data
    }

    fun addGlobal(global: GlobalValue) {
        globals.add(global)
    }

    fun addStructType(structType: StructType) = structs.add(structType)

    fun findStructType(name: String): StructType {
        return structs.find { it.name == name } ?: throw RuntimeException("not found name=$name")
    }

    fun createExternFunction(functionName: SymbolValue, returnType: ElementaryTypeToken, arguments: List<ElementaryTypeToken>): ExternFunction {
        val extern = ExternFunction(functionName.name, returnType.type(), arguments.map { it.type() })
        externFunctions.add(extern)
        return extern
    }

    fun build(): Module {
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