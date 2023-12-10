package ir.read.bulder

import ir.*
import ir.read.*
import ir.module.*
import ir.pass.ana.VerifySSA


class ModuleBuilderWithContext {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()
    private val constants = mutableSetOf<GlobalValue>()

    fun createFunction(name: SymbolValue, returnType: ElementaryTypeToken, argumentTypes: List<ElementaryTypeToken>, argumentValues: List<LocalValueToken>): FunctionDataBuilderWithContext {
        val data = FunctionDataBuilderWithContext.create(name, returnType, argumentTypes, argumentValues, constants)
        functions.add(data)
        return data
    }

    fun addGlobal(global: GlobalValue) {
        constants.add(global)
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

        val ssa = SSAModule(fns, externFunctions, constants)
        return VerifySSA.run(ssa)
    }

    companion object {
        fun create() : ModuleBuilderWithContext {
            return ModuleBuilderWithContext()
        }
    }
}