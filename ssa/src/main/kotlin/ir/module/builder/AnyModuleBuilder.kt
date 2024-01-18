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

    abstract fun build(): Module
}