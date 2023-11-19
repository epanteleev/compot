package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.Type

enum class IntPredicate {
    Eq {
        override fun toString(): String {
            return "eq"
        }
    },
    Ne {
        override fun toString(): String {
            return "ne"
        }
    },
    Ugt {
        override fun toString(): String {
            return "ugt"
        }
    },
    Uge {
        override fun toString(): String {
            return "uge"
        }
    },
    Ult {
        override fun toString(): String {
            return "ult"
        }
    },
    Ule {
        override fun toString(): String {
            return "ule"
        }
    },
    Sgt {
        override fun toString(): String {
            return "sgt"
        }
    },
    Sge {
        override fun toString(): String {
            return "sge"
        }
    },
    Slt {
        override fun toString(): String {
            return "slt"
        }
    },
    Sle {
        override fun toString(): String {
            return "sle"
        }
    };

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

class IntCompare private constructor(name: String, a: Value, private val predicate: IntPredicate, b: Value) :
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
        assert(newUsages.size == 2) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, newUsages[0], predicate, newUsages[1])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, a: Value, predicate: IntPredicate, b: Value): IntCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same types, but a.type=$aType, b.type=$bType"
            }

            return registerUser(IntCompare(name, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType
        }

        fun isCorrect(icmp: IntCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}
