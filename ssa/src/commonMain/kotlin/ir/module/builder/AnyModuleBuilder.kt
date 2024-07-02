package ir.module.builder

import ir.global.GlobalConstant
import ir.global.GlobalSymbol
import ir.global.GlobalValue
import ir.module.ExternFunction
import ir.module.Module
import ir.types.*


abstract class AnyModuleBuilder {
    protected val constantPool = hashMapOf<String, GlobalConstant>()
    protected val globals = hashMapOf<String, GlobalValue>()
    protected val structs = hashMapOf<String, StructType>()
    protected val externFunctions = hashMapOf<String, ExternFunction>()

    fun addGlobal(name: String, data: GlobalConstant): GlobalValue {
        val has = globals.put(name, GlobalValue(name, data))
        if (has != null) {
            throw IllegalArgumentException("global with name='$name' already exists")
        }

        return globals[name]!!
    }

    fun addConstant(global: GlobalConstant): GlobalConstant {
        val has = constantPool.put(global.name(), global)
        if (has != null) {
            throw IllegalArgumentException("global with name='${global.name()}' already exists")
        }

        return global
    }

    fun structType(name: String, fields: List<NonTrivialType>): StructType {
        val structType = StructType(name, fields)
        val has = structs.put(name, structType)
        if (has != null) {
            throw IllegalArgumentException("struct with name='$name' already exists")
        }

        return structType
    }

    fun createExternFunction(name: String, returnType: Type, arguments: List<Type>, isVararg: Boolean = false): ExternFunction {
        val extern = ExternFunction(name, returnType, arguments, isVararg)
        val has = externFunctions.put(name, extern)
        if (has != null) {
            throw IllegalArgumentException("extern function with name='$name' already exists")
        }

        return extern
    }

    fun findConstantOrNull(name: String): GlobalSymbol? {
        return constantPool[name]
    }

    fun findGlobalOrNull(name: String): GlobalValue? {
        return globals[name]
    }

    fun findStructType(name: String): StructType {
        return findStructTypeOrNull(name) ?: throw NoSuchElementException("not found name=$name")
    }

    fun findStructTypeOrNull(name: String): StructType? {
        return structs[name]
    }

    abstract fun build(): Module
}