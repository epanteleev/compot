package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class PointerCompare private constructor(name: String, a: Value, private val predicate: IntPredicate, b: Value) :
    CompareInstruction(name, a, b) {
    override fun dump(): String {
        return "%$identifier = $NAME $predicate ${first().type()} ${first()}, ${second()}"
    }

    override fun predicate(): IntPredicate = predicate

    override fun operandsType(): PointerType = Type.Ptr

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "pcmp"

        fun make(name: String, a: Value, predicate: IntPredicate, b: Value): PointerCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same types, but a.type=$aType, b.type=$bType"
            }

            return registerUser(PointerCompare(name, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && aType is PointerType
        }

        fun isCorrect(icmp: PointerCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}