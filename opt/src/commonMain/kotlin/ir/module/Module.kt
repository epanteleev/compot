package ir.module

import ir.global.AnyGlobalValue
import ir.global.GlobalConstant
import ir.module.auxiliary.DumpSSAModule
import ir.types.StructType

class SSAModule(functions: Map<String, FunctionData>,
                val externFunctions: Map<String, DirectFunctionPrototype>,
                val constantPool: MutableMap<String, GlobalConstant>,
                val globals: Map<String, AnyGlobalValue>,
                val types: Map<String, StructType>):
    Module<FunctionData>(functions) {

    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.values + functions.values.map { it.prototype }
    }

    fun findFunction(name: String): FunctionData {
        return functions[name] ?: throw NoSuchElementException("Cannot find function: $name")
    }

    fun findConstant(name: String): GlobalConstant {
        return constantPool[name] ?: throw NoSuchElementException("Cannot find function: $name")
    }

    fun addConstant(constant: GlobalConstant): GlobalConstant {
        val old = constantPool.put(constant.name(), constant) ?: return constant
        throw IllegalArgumentException("Constant with name ${constant.name()} already exists: $old")
    }

    override fun copy(): SSAModule {
        val newMap = hashMapOf<String, FunctionData>()
        for ((name, function) in functions) {
            newMap[name] = function.copy()
        }

        return SSAModule(newMap, externFunctions, constantPool, globals, types) //TODO deep copy constantPool
    }

    override fun toString(): String {
        return DumpSSAModule(this).toString()
    }
}