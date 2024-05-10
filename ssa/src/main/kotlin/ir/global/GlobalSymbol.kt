package ir.global

import ir.Value
import ir.types.NonTrivialType


interface GlobalSymbol: Value {
    fun name(): String
    fun dump(): String
    override fun type(): NonTrivialType
}

interface FunctionSymbol: GlobalSymbol