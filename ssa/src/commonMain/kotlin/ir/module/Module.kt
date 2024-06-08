package ir.module

import ir.global.GlobalSymbol
import ir.module.auxiliary.CopyCFG
import ir.module.auxiliary.DumpModule
import ir.types.StructType

data class ModuleException(override val message: String): Exception(message)

abstract class Module(internal open val functions: List<FunctionData>,
                      internal open val externFunctions: Set<ExternFunction>,
                      internal open val globals: Set<GlobalSymbol>,
                      internal open val types: List<StructType>) {
    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.toList() + functions.map { it.prototype }
    }

    fun findFunction(prototype: FunctionPrototype): FunctionData {
        return functions.find { it.prototype == prototype }
            ?: throw ModuleException("Cannot find function: $prototype")
    }

    fun findGlobal(name: String): GlobalSymbol {
        return globals.find { it.name() == name } //TODO O(n)
            ?: throw ModuleException("Cannot find function: $name")
    }

    abstract fun copy(): Module

    fun functions(): Iterator<FunctionData> {
        return functions.iterator()
    }
}

data class SSAModule(override val functions: List<FunctionData>,
                     override val externFunctions: Set<ExternFunction>,
                     override val globals: Set<GlobalSymbol>,
                     override val types: List<StructType>):
    Module(functions, externFunctions, globals, types) {
    override fun copy(): Module {
        return SSAModule(functions.map { CopyCFG.copy(it) }, externFunctions, globals, types)
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}