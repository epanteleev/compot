package ir.platform.x64

import ir.global.*
import ir.module.*
import ir.types.StructType
import ir.platform.x64.auxiliary.DumpLModule


class LModule(functions: Map<String, FunctionData>,
              externFunctions: Map<String, DirectFunctionPrototype>,
              constantPool: Map<String, GlobalConstant>,
              globals: Map<String, AnyGlobalValue>,
              types: Map<String, StructType>):
    Module(functions, externFunctions, constantPool, globals, types) {

    override fun copy(): Module {
        val newMap = hashMapOf<String, FunctionData>()
        for ((name, function) in functions) {
            newMap[name] = function.copy()
        }
        return LModule(newMap, functionDeclarations, constantPool, globals, types) //TODO deep copy???
    }

    override fun toString(): String {
        return DumpLModule(this).toString()
    }
}