package ir.read.bulder

import ir.*
import ir.module.SSAModule
import ir.module.Module
import ir.pass.ana.VerifySSA
import ir.read.Identifier
import ir.read.TypeToken
import ir.read.ValueInstructionToken

class ModuleBuilderWithContext {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()

    fun createFunction(name: Identifier, returnType: TypeToken, argumentTypes: List<TypeToken>, argumentValues: List<ValueInstructionToken>): FunctionDataBuilderWithContext {
        val data = FunctionDataBuilderWithContext.create(name, returnType, argumentTypes, argumentValues)
        functions.add(data)
        return data
    }

    fun createExternFunction(name: Identifier, returnType: TypeToken, arguments: List<TypeToken>): ExternFunction {
        val extern = ExternFunction(name.string, returnType.type(), arguments.map { it.type() })
        externFunctions.add(extern)
        return extern
    }

    fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        val ssa = SSAModule(fns, externFunctions)
        return VerifySSA.run(ssa)
    }

    companion object {
        fun create() : ModuleBuilderWithContext {
            return ModuleBuilderWithContext()
        }
    }
}