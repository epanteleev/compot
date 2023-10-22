package ir.instruction

import ir.Value
import ir.types.Type

enum class IntPredicate {
    Eq,
    Ne,
    Ugt,
    Uge,
    Ult,
    Ule,
    Sgt,
    Sge,
    Slt,
    Sle;

    override fun toString(): String {
        val name = when (this) {
            Eq  -> "eq"
            Ne  -> "ne"
            Uge -> "uge"
            Ugt -> "ugt"
            Ult -> "ult"
            Ule -> "ule"
            Sgt -> "sgt"
            Sge -> "sge"
            Slt -> "slt"
            Sle -> "sle"
        }
        return name
    }

    fun invert(): IntPredicate {
        return when (this) {
            Eq  -> Ne
            Ne  -> Eq
            Uge -> Ult
            Ugt -> Ule
            Ult -> Uge
            Ule -> Ugt
            Sgt -> Sle
            Sge -> Slt
            Slt -> Sge
            Sle -> Sgt
        }
    }
}

class IntCompare(name: String, a: Value, private val predicate: IntPredicate, b: Value) :
    ValueInstruction(name, Type.U1, arrayOf(a, b)) {
    override fun dump(): String {
        return "%$identifier = icmp $predicate ${first().type()} ${first()}, ${second()}"
    }

    fun predicate(): IntPredicate {
        return predicate
    }

    fun first(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun second(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun copy(newUsages: List<Value>): IntCompare {
        return IntCompare(identifier, newUsages[0], predicate, newUsages[1])
    }
}
