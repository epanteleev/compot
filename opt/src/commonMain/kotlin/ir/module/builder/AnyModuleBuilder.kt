package ir.module.builder

import ir.types.*
import ir.global.*
import ir.module.*
import ir.value.UsableValue
import ir.attributes.FunctionAttribute
import ir.attributes.GlobalValueAttribute
import ir.value.asValue
import ir.value.constant.NonTrivialConstant


abstract class AnyModuleBuilder {
    protected val constantPool = hashMapOf<String, GlobalConstant>()
    protected val globals = linkedMapOf<String, AnyGlobalValue>()
    protected val structs = hashMapOf<String, StructType>()
    protected val externFunctions = hashMapOf<String, ExternFunction>()

    fun addGlobalValue(name: String, initializer: NonTrivialConstant, attributes: GlobalValueAttribute): GlobalValue {
        val global = GlobalValue.create(name, initializer, attributes)
        val has = globals.put(name, global)
        if (has != null) {
            throw IllegalArgumentException("global with name='$name' already exists")
        }

        return global
    }

    fun redefineGlobalValue(oldGlobalValue: AnyGlobalValue, name: String, initializer: NonTrivialConstant, attributes: GlobalValueAttribute): GlobalValue {
        val removed = globals.remove(oldGlobalValue.name()) ?: throw IllegalArgumentException("global with name='${oldGlobalValue.name()}' not found")
        val global = GlobalValue.create(name, initializer, attributes)
        globals[name] = global
        UsableValue.updateUsages(removed.asValue()) { global }

        return global
    }

    fun addGlobalValue(name: String, data: NonTrivialConstant): GlobalValue {
        return addGlobalValue(name, data, GlobalValueAttribute.DEFAULT)
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

    fun createExternFunction(name: String, returnType: Type, arguments: List<NonTrivialType>, attributes: Set<FunctionAttribute> = hashSetOf()): ExternFunction {
        val extern = ExternFunction(name, returnType, arguments, attributes)
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