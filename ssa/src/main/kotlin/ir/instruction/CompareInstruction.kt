package ir.instruction

import ir.Value
import ir.types.Type

abstract class CompareInstruction(name: String, first: Value, second: Value) :
    ValueInstruction(name, Type.U1, arrayOf(first, second))
