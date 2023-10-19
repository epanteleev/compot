package ir.module

import ir.AnyFunctionPrototype
import ir.ExternFunction
import ir.FunctionPrototype
import ir.module.auxiliary.Copy
import ir.module.auxiliary.DumpModule

data class ModuleException(override val message: String): Exception(message)

abstract class Module(internal open val functions: List<FunctionData>, internal open val externFunctions: Set<ExternFunction>) {
    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.toList() + functions.map { it.prototype }
    }

    fun findFunction(prototype: FunctionPrototype): FunctionData {
        return functions.find { it.prototype == prototype }
            ?: throw ModuleException("Cannot find function: $prototype")
    }

    abstract fun copy(): Module

    fun functions(): Iterator<FunctionData> {
        return functions.iterator()
    }
}

data class SSAModule(override val functions: List<FunctionData>, override val externFunctions: Set<ExternFunction>):
    Module(functions, externFunctions) {
    override fun copy(): Module {
        return SSAModule(functions.map { Copy.copy(it) }, externFunctions.mapTo(mutableSetOf()) { it })
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}