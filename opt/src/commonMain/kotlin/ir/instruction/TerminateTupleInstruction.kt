package ir.instruction

import ir.types.*
import ir.value.*
import ir.module.block.Block


sealed class TerminateTupleInstruction(id: Identity,
                                         owner: Block,
                                         tp: TrivialType,
                                         operands: Array<Value>,
                                         targets: Array<Block>):
    TerminateValueInstruction(id, owner, tp, operands, targets), TupleValue {

    abstract override fun type(): TupleType
}