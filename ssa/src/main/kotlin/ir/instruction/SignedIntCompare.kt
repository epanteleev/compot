package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class SignedIntCompare private constructor(name: String, a: Value, private val predicate: IntPredicate, b: Value) :
    CompareInstruction(name, a, b) {
    override fun dump(): String {
        return "%$identifier = $NAME $predicate ${first().type()} ${first()}, ${second()}"
    }

    override fun predicate(): IntPredicate = predicate

    override fun operandsType(): SignedIntType {
        val opType = first().type()
        assert(opType is SignedIntType) {
            "should be, but opType=$opType"
        }

        return opType as SignedIntType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "icmp"

        fun make(name: String, a: Value, predicate: IntPredicate, b: Value): SignedIntCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same types in '$name', but a=$a:$aType, b=$b:$bType"
            }

            return registerUser(SignedIntCompare(name, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && aType is IntegerType
        }

        fun typeCheck(icmp: SignedIntCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}