package ir.read.bulder

import ir.types.StructType


interface TypeResolver {
    fun resolve(name: String): StructType
}