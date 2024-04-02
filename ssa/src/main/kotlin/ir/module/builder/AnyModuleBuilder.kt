package ir.module.builder

import ir.GlobalSymbol
import ir.GlobalValue
import ir.module.Module
import ir.types.StructType


abstract class AnyModuleBuilder {
    protected val globals = mutableSetOf<GlobalSymbol>()
    protected val structs = arrayListOf<StructType>()

    fun addConstant(global: GlobalValue): GlobalValue {
        globals.add(global)
        return global
    }

    fun addStructType(structType: StructType): StructType {
        structs.add(structType)
        return structType
    }

    fun findGlobal(name: String): GlobalSymbol {
        return globals.find { it.name() == name } ?: throw RuntimeException("not found name=$name") //TODO O(n)
    }

    fun findStructType(name: String): StructType {
        return structs.find { it.name == name } ?: throw RuntimeException("not found name=$name") //TODO O(n)
    }

    abstract fun build(): Module
}