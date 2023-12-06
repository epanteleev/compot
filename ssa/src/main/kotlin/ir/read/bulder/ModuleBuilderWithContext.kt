package ir.read.bulder

import ir.ExternFunction
import ir.GlobalConstant
import ir.module.Module
import ir.module.SSAModule
import ir.pass.ana.VerifySSA
import ir.read.ElementaryTypeToken
import ir.read.FunctionName
import ir.read.ValueInstructionToken

class ModuleBuilderWithContext {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()
    private val constants = mutableSetOf<GlobalConstant>()

    fun createFunction(name: FunctionName, returnType: ElementaryTypeToken, argumentTypes: List<ElementaryTypeToken>, argumentValues: List<ValueInstructionToken>): FunctionDataBuilderWithContext {
        val data = FunctionDataBuilderWithContext.create(name, returnType, argumentTypes, argumentValues)
        functions.add(data)
        return data
    }

    fun createExternFunction(functionName: FunctionName, returnType: ElementaryTypeToken, arguments: List<ElementaryTypeToken>): ExternFunction {
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