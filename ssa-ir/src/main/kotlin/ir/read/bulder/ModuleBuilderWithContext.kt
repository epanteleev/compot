package ir.read.bulder

import ir.*
import ir.read.*
import ir.module.*
import ir.pass.ana.VerifySSA

class ModuleBuilderWithContext {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()

    fun createFunction(name: FunctionName, returnType: PrimitiveTypeToken, argumentTypes: List<PrimitiveTypeToken>, argumentValues: List<ValueInstructionToken>): FunctionDataBuilderWithContext {
        val data = FunctionDataBuilderWithContext.create(name, returnType, argumentTypes, argumentValues)
        functions.add(data)
        return data
    }

    fun createExternFunction(functionName: FunctionName, returnType: PrimitiveTypeToken, arguments: List<PrimitiveTypeToken>): ExternFunction {
        val extern = ExternFunction(functionName.name, returnType.type(), arguments.map { it.type() })
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