package ir.instruction

import ir.value.Value
import ir.module.block.Block
import ir.types.ArithmeticType


abstract class ArithmeticUnary(id: Identity, owner: Block, tp: ArithmeticType, value: Value):
    ValueInstruction(id, owner, tp, arrayOf(value))