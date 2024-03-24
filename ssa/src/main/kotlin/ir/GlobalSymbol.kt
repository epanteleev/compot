package ir

import ir.types.NonTrivialType
import ir.types.Type

interface GlobalSymbol: Value {
    fun name(): String
    fun dump(): String

    override fun type(): NonTrivialType = Type.Ptr
}