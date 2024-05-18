package ir.instruction

import ir.Value
import ir.module.block.Block
import ir.types.ArithmeticType


abstract class ArithmeticUnary(name: String, owner: Block, tp: ArithmeticType, value: Value):
    ValueInstruction(name, owner, tp, arrayOf(value))