package ir.instruction

import common.assertion
import ir.Value
import ir.module.block.Block
import ir.types.*

interface AnyPredicateType {
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


abstract class CompareInstruction(id: Identity, owner: Block, first: Value, second: Value) :
    ValueInstruction(id, owner, Type.U1, arrayOf(first, second)) {
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
}
