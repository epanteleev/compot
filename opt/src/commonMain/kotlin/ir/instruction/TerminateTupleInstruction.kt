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

    fun proj(index: Int): Projection? {
        for (user in usedIn()) {
            user as Projection
            if (user.index() == index) {
                return user
            }
        }
        return null
    }

    abstract override fun type(): TupleType
}