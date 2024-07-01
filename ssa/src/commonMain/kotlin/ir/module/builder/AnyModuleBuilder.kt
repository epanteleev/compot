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
        val has = globals.put(global.name(), global)
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

    fun findGlobal(name: String): GlobalSymbol {
        return globals[name] ?: throw NoSuchElementException("not found name=$name")
    }

    fun findStructType(name: String): StructType {
        return findStructTypeOrNull(name) ?: throw NoSuchElementException("not found name=$name")
    }

    fun findStructTypeOrNull(name: String): StructType? {
        return structs[name]
    }

    abstract fun build(): Module
}