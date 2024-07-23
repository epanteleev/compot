package ir.module

import ir.global.GlobalConstant
import ir.global.GlobalSymbol
import ir.global.GlobalValue
import ir.module.auxiliary.CopyCFG
import ir.module.auxiliary.DumpModule
import ir.types.StructType

abstract class Module(internal val functions: List<FunctionData>,
                      internal val externFunctions: Map<String, ExternFunction>,
                      internal val constantPool: Map<String, GlobalConstant>,
                      internal val globals: Map<String, GlobalValue>,
                      internal val types: Map<String, StructType>) {
    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.values + functions
    }

    fun findFunction(name: String): FunctionData {
        return functions.find { it.name() == name }
            ?: throw NoSuchElementException("Cannot find function: $name")
    }

    fun findConstant(name: String): GlobalConstant {
        return constantPool[name] ?: throw NoSuchElementException("Cannot find function: $name")
    }

    abstract fun copy(): Module

    fun functions(): List<FunctionData> {
        return functions
    }
}

class SSAModule(functions: List<FunctionData>,
                externFunctions: Map<String, ExternFunction>,
                constantPool: Map<String, GlobalConstant>,
                globals: Map<String, GlobalValue>,
                types: Map<String, StructType>):
    Module(functions, externFunctions, constantPool, globals, types) {
    override fun copy(): Module {
        return SSAModule(functions.map { CopyCFG.copy(it) }, externFunctions, constantPool, globals, types)
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}