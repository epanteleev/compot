package ir.instruction

import common.assertion
import ir.value.Value
import ir.module.block.Block
import ir.types.*

sealed interface AnyPredicateType {
    fun invert(): AnyPredicateType
}

enum class IntPredicate: AnyPredicateType {
    Eq {
        override fun toString(): String = "eq"
        override fun invert(): IntPredicate = Ne
    },
    Ne {
        override fun toString(): String = "ne"
        override fun invert(): IntPredicate = Eq
    },
    Gt {
        override fun toString(): String = "gt"
        override fun invert(): IntPredicate = Le
    },
    Ge {
        override fun toString(): String = "ge"
        override fun invert(): IntPredicate = Lt
    },
    Lt {
        override fun toString(): String = "lt"
        override fun invert(): IntPredicate = Ge
    },
    Le {
        override fun toString(): String = "le"
        override fun invert(): IntPredicate = Gt
    };
}


sealed class CompareInstruction(id: Identity, owner: Block, val operandsType: PrimitiveType, first: Value, second: Value) :
    ValueInstruction(id, owner, Type.U1, arrayOf(first, second)) {
    fun operandsType(): PrimitiveType = operandsType
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

    override fun type(): FlagType = tp as FlagType
}
