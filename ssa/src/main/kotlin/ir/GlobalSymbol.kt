package ir

import ir.types.Type

interface GlobalSymbol: Value {
    fun name(): String
    fun dump(): String

    override fun type(): Type = Type.Ptr
}