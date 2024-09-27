package ir.instruction

import ir.module.block.Block
import ir.types.TrivialType
import ir.types.TupleType
import ir.value.LocalValue
import ir.value.TupleValue
import ir.value.Value


abstract class TerminateTupleInstruction(id: Identity,
                                         owner: Block,
                                         tp: TrivialType,
                                         operands: Array<Value>,
                                         targets: Array<Block>):
    TerminateValueInstruction(id, owner, tp, operands, targets), TupleValue {

    abstract override fun type(): TupleType
}