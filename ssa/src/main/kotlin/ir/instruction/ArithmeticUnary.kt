package ir.instruction

import ir.Value
import ir.types.ArithmeticType


abstract class ArithmeticUnary(name: String, tp: ArithmeticType, value: Value):
    ValueInstruction(name, tp, arrayOf(value))