package ir.global

import ir.instruction.Instruction
import ir.value.Value
import ir.types.NonTrivialType
import ir.value.UsableValue


sealed interface GlobalSymbol: Value {
    fun name(): String
    fun dump(): String
    override fun type(): NonTrivialType
}

interface FunctionSymbol: GlobalSymbol

sealed class AnyGlobalValue: GlobalSymbol, UsableValue {
    final override var usedIn: MutableList<Instruction> = mutableListOf()
}