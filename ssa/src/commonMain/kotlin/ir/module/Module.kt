package ir.module

import ir.global.GlobalSymbol
import ir.module.auxiliary.CopyCFG
import ir.module.auxiliary.DumpModule
import ir.types.StructType

abstract class Module(internal val functions: List<FunctionData>,
                      internal val externFunctions: Map<String, ExternFunction>,
                      internal val globals: Map<String, GlobalSymbol>,
                      internal val types: Map<String, StructType>) {
    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.values + functions.map { it.prototype }
    }

    fun findFunction(prototype: FunctionPrototype): FunctionData {
        return functions.find { it.prototype == prototype }
            ?: throw NoSuchElementException("Cannot find function: $prototype")
    }

    fun findGlobal(name: String): GlobalSymbol {
        return globals[name] ?: throw NoSuchElementException("Cannot find function: $name")
    }

    abstract fun copy(): Module

    fun functions(): Iterator<FunctionData> {
        return functions.iterator()
    }
}

class SSAModule(functions: List<FunctionData>,
                externFunctions: Map<String, ExternFunction>,
                globals: Map<String, GlobalSymbol>,
                types: Map<String, StructType>):
    Module(functions, externFunctions, globals, types) {
    override fun copy(): Module {
        return SSAModule(functions.map { CopyCFG.copy(it) }, externFunctions, globals, types)
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}