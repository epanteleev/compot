package ir.instruction

import ir.module.block.Block
import ir.types.TrivialType
import ir.types.TupleType
import ir.value.LocalValue
import ir.value.Value


abstract class TerminateTupleInstruction(id: Identity,
                                         owner: Block,
                                         tp: TrivialType,
                                         operands: Array<Value>,
                                         targets: Array<Block>):
    TerminateValueInstruction(id, owner, tp, operands, targets), LocalValue {

    abstract override fun type(): TupleType
}