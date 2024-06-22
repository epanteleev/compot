package ir.module.builder

import ir.global.GlobalConstant
import ir.global.GlobalSymbol
import ir.global.GlobalValue
import ir.module.Module
import ir.types.NonTrivialType
import ir.types.StructType


abstract class AnyModuleBuilder {
    protected val globals = hashMapOf<String, GlobalSymbol>()
    protected val structs = hashMapOf<String, StructType>()

    fun addConstant(global: GlobalConstant): GlobalConstant {
        if (globals.containsKey(global.name())) {
            throw IllegalArgumentException("global with name='${global.name()}' already exists")
        }
        globals[global.name()] = global
        return global
    }

    fun structType(name: String, fields: List<NonTrivialType>): StructType {
        if (structs.containsKey(name)) {
            throw IllegalArgumentException("struct with name='$name' already exists")
        }
        val structType = StructType(name, fields)
        structs[structType.name] = structType
        return structType
    }

    fun findGlobal(name: String): GlobalSymbol {
        return globals[name] ?: throw NoSuchElementException("not found name=$name")
    }

    fun findStructType(name: String): StructType {
        return structs[name] ?: throw NoSuchElementException("not found name=$name")
    }

    abstract fun build(): Module
}