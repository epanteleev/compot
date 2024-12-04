package ir.instruction

import ir.value.Value
import ir.module.block.Block
import ir.types.ArithmeticType


sealed class ArithmeticUnary(id: Identity, owner: Block, protected val tp: ArithmeticType, value: Value):
    ValueInstruction(id, owner, arrayOf(value))