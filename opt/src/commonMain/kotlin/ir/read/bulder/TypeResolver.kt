package ir.read.bulder

import ir.types.StructType


sealed interface TypeResolver {
    fun resolve(name: String): StructType
}