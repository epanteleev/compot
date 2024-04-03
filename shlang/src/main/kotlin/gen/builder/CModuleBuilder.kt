package gen.builder

import ir.ExternFunction
import ir.FunctionPrototype
import ir.module.Module
import ir.module.SSAModule
import ir.module.builder.AnyModuleBuilder
import ir.module.builder.impl.FunctionDataBuilder
import ir.pass.ana.VerifySSA
import ir.read.LocalValueToken
import ir.read.SymbolValue
import ir.read.TypeToken
import ir.read.bulder.FunctionDataBuilderWithContext
import ir.types.Type


//class CModuleBuilder: AnyModuleBuilder() {
//    private val functions = arrayListOf<CFunctionDataBuilder>()
//    private val externFunctions = mutableSetOf<ExternFunction>()
//
//    override fun build(): Module {
//        val fns = functions.mapTo(arrayListOf()) {
//            it.build()
//        }
//
//        val ssa = SSAModule(fns, externFunctions, globals, structs)
//        return VerifySSA.run(ssa)
//    }
//
////    fun createFunction(functionName: SymbolValue, returnType: TypeToken, argumentTypes: List<Type>, argumentValues: List<String>): CFunctionDataBuilder {
////        val args        = argumentTypes.mapTo(arrayListOf()) { it.type(this) }
////        val prototype   = FunctionPrototype(functionName.name, returnType.type(this), args)
////
////        val data = FunctionDataBuilderWithContext.create(this, prototype, argumentValues)
////        functions.add(data)
////        globals.add(prototype)
////        return data
////    }
//
//    companion object {
//        fun create() : CModuleBuilder {
//            return CModuleBuilder()
//        }
//    }
//}