package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block


sealed interface AnyPredicateType {
    fun invert(): AnyPredicateType
}

sealed class CompareInstruction(id: Identity, owner: Block, val operandsType: PrimitiveType, first: Value, second: Value) :
    ValueInstruction(id, owner, arrayOf(first, second)) {

    abstract fun operandsType(): PrimitiveType
    abstract fun predicate(): AnyPredicateType

    fun first(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun second(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun type(): FlagType = FlagType
}
