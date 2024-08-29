package ir.module.builder

import ir.attributes.GlobalValueAttribute
import ir.global.*
import ir.module.ExternFunction
import ir.module.Module
import ir.types.*
import ir.value.Constant


abstract class AnyModuleBuilder {
    protected val constantPool = hashMapOf<String, GlobalConstant>()
    protected val globals = hashMapOf<String, AnyGlobalValue>()
    protected val structs = hashMapOf<String, StructType>()
    protected val externFunctions = hashMapOf<String, ExternFunction>()

    fun addGlobal(name: String, type: NonTrivialType, data: Constant, attributes: GlobalValueAttribute): GlobalValue {
        val global = GlobalValue(name, type, data, attributes)
        val has = globals.put(name, global)
        if (has != null) {
            throw IllegalArgumentException("global with name='$name' already exists")
        }

        return global
    }

    fun addGlobal(name: String, type: NonTrivialType, data: Constant): GlobalValue {
        return addGlobal(name, type, data, GlobalValueAttribute.DEFAULT)
    }

    fun addExternValue(name: String, type: NonTrivialType): ExternValue {
        val global = ExternValue(name, type)
        val has = globals.put(name, global)
        if (has != null) {
            throw IllegalArgumentException("global with name='$name' already exists")
        }

        return global
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

    fun createExternFunction(name: String, returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean = false): ExternFunction {
        val extern = ExternFunction(name, returnType, arguments, isVararg)
        val has = externFunctions.put(name, extern)
        if (has != null) {
            throw IllegalArgumentException("extern function with name='$name' already exists")
        }

        return extern
    }

    fun findExternFunctionOrNull(name: String): ExternFunction? {
        return externFunctions[name]
    }

    fun findConstantOrNull(name: String): GlobalSymbol? {
        return constantPool[name]
    }

    fun findGlobalOrNull(name: String): AnyGlobalValue? {
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