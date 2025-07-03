package ir.platform.x64

import ir.global.*
import ir.module.*
import ir.types.StructType
import ir.platform.x64.auxiliary.DumpLModule


class LModule(functions: Map<String, FunctionData>,
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

    override fun copy(): LModule {
        val newMap = hashMapOf<String, FunctionData>()
        for ((name, function) in functions) {
            newMap[name] = function.copy()
        }
        return LModule(newMap, externFunctions, constantPool, globals, types) //TODO deep copy???
    }

    override fun toString(): String {
        return DumpLModule(this).toString()
    }
}