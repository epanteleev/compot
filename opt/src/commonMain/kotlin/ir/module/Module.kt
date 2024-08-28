package ir.module

import ir.global.AnyGlobalValue
import ir.global.GlobalConstant
import ir.global.GlobalValue
import ir.module.auxiliary.DumpModule
import ir.types.StructType

abstract class Module(internal val functions: Map<String, FunctionData>,
                      internal val externFunctions: Map<String, ExternFunction>,
                      internal val constantPool: Map<String, GlobalConstant>,
                      internal val globals: Map<String, AnyGlobalValue>,
                      internal val types: Map<String, StructType>) {
    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.values + functions.values.map { it.prototype }
    }

    fun findFunction(name: String): FunctionData {
        return functions[name] ?: throw NoSuchElementException("Cannot find function: $name")
    }

    fun findConstant(name: String): GlobalConstant {
        return constantPool[name] ?: throw NoSuchElementException("Cannot find function: $name")
    }

    abstract fun copy(): Module

    fun functions(): Collection<FunctionData> {
        return functions.values
    }
}

class SSAModule(functions: Map<String, FunctionData>,
                externFunctions: Map<String, ExternFunction>,
                constantPool: Map<String, GlobalConstant>,
                globals: Map<String, AnyGlobalValue>,
                types: Map<String, StructType>):
    Module(functions, externFunctions, constantPool, globals, types) {
    override fun copy(): Module {
        val newMap = hashMapOf<String, FunctionData>()
        for ((name, function) in functions) {
            newMap[name] = function.copy()
        }

        return SSAModule(newMap, externFunctions, constantPool, globals, types)
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}