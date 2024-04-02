package gen.builder

import ir.ExternFunction
import ir.FunctionPrototype
import ir.module.Module
import ir.module.SSAModule
import ir.module.builder.AnyModuleBuilder
import ir.module.builder.impl.FunctionDataBuilder
import ir.pass.ana.VerifySSA
import ir.types.Type


class CModuleBuilder: AnyModuleBuilder() {
    private val functions = arrayListOf<FunctionDataBuilder>()
    private val externFunctions = mutableSetOf<ExternFunction>()

    override fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        val ssa = SSAModule(fns, externFunctions, globals, structs)
        return VerifySSA.run(ssa)
    }

    companion object {
        fun create() : CModuleBuilder {
            return CModuleBuilder()
        }
    }
}