package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class UnsignedIntCompare private constructor(name: String, a: Value, private val predicate: IntPredicate, b: Value) :
    CompareInstruction(name, a, b) {
    override fun dump(): String {
        return "%$identifier = $NAME $predicate ${first().type()} ${first()}, ${second()}"
    }

    override fun predicate(): IntPredicate = predicate

    override fun operandsType(): UnsignedIntType {
        val opType = first().type()
        assert(opType is UnsignedIntType) {
            "should be, but opType=$opType"
        }

        return first().type() as UnsignedIntType
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "ucmp"

        fun make(name: String, a: Value, predicate: IntPredicate, b: Value): UnsignedIntCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same unsigned integer types, but a.type=$aType, b.type=$bType"
            }

            return registerUser(UnsignedIntCompare(name, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && aType is UnsignedIntType
        }

        fun typeCheck(icmp: UnsignedIntCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}