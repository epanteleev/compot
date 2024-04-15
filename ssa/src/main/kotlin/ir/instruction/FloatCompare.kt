package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


enum class FloatPredicate: AnyPredicateType {
    Oeq { // ordered and equal
        override fun invert(): FloatPredicate = One
        override fun toString(): String = "oeq"
    },
    Ogt { // ordered and greater than
        override fun invert(): FloatPredicate = Ole
        override fun toString(): String = "ogt"
    },
    Oge { // ordered and greater than or equal
        override fun invert(): FloatPredicate = Olt
        override fun toString(): String = "oge"
    },
    Olt { // ordered and less than
        override fun invert(): FloatPredicate = Oge
        override fun toString(): String = "olt"
    },
    Ole { // ordered and less than or equal
        override fun invert(): FloatPredicate = Ogt
        override fun toString(): String = "ole"
    },
    One { // ordered and not equal
        override fun invert(): FloatPredicate = Oeq
        override fun toString(): String = "one"
    },
    Ord { // ordered (no nans)
        override fun invert(): FloatPredicate = TODO()
        override fun toString(): String = "ord"
    },
    Ueq { // unordered or equal
        override fun invert(): FloatPredicate = Une
        override fun toString(): String = "ueq"
    },
    Ugt { // unordered or greater than
        override fun toString(): String = "ugt"
        override fun invert(): FloatPredicate = Ule
    },
    Uge { // unordered or greater than or equal
        override fun toString(): String = "uge"
        override fun invert(): FloatPredicate = Ult
    },
    Ult { // unordered or less than
        override fun toString(): String = "ult"
        override fun invert(): FloatPredicate = Uge
    },
    Ule { // unordered or less than or equal
        override fun toString(): String = "ule"
        override fun invert(): FloatPredicate = Ugt
    },
    Une { // unordered or not equal
        override fun invert(): FloatPredicate = Ueq
        override fun toString(): String = "une"
    },
    Uno { // unordered (either nans)
        override fun invert(): FloatPredicate = TODO()
        override fun toString(): String = "uno"
    };
}

class FloatCompare private constructor(name: String, a: Value, private val predicate: FloatPredicate, b: Value) :
    CompareInstruction(name, a, b) {
    override fun dump(): String {
        return "%$identifier = $NAME $predicate ${first().type()} ${first()}, ${second()}"
    }

    override fun predicate(): FloatPredicate = predicate

    override fun operandsType(): FloatingPointType {
        val opType = first().type()
        assert(opType is FloatingPointType) {
            "should be, but opType=$opType"
        }

        return first().type() as FloatingPointType
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "fcmp"

        fun make(name: String, a: Value, predicate: FloatPredicate, b: Value): FloatCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same types, but a=$a:$aType, b=$b:$bType"
            }

            return registerUser(FloatCompare(name, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && aType is FloatingPointType
        }

        fun typeCheck(icmp: FloatCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}