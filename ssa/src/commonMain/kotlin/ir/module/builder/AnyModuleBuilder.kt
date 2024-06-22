package ir.module.builder

import ir.global.GlobalConstant
import ir.global.GlobalSymbol
import ir.global.GlobalValue
import ir.module.Module
import ir.types.StructType


abstract class AnyModuleBuilder {
    protected val globals = hashMapOf<String, GlobalSymbol>()
    protected val structs = hashMapOf<String, StructType>()

    fun addConstant(global: GlobalConstant): GlobalConstant {
        globals[global.name()] = global
        return global
    }

    fun addStructType(structType: StructType): StructType {
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