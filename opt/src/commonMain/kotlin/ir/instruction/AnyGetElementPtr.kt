package ir.instruction

import ir.module.block.Block
import ir.types.NonTrivialType
import ir.value.Value


sealed class AnyGetElementPtr(id: Identity, owner: Block, operands: Array<Value>):
    ValueInstruction(id, owner, operands) {

    abstract fun source(): Value

    fun source(newSource: Value) = owner.df {
        update(SOURCE, newSource)
    }

    abstract fun index(): Value
    abstract fun accessType(): NonTrivialType

    companion object {
        private const val SOURCE = 0
    }
}