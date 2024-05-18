package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class UnsignedIntCompare private constructor(id: Identity, owner: Block, a: Value, private val predicate: IntPredicate, b: Value) :
    CompareInstruction(id, owner, a, b) {
    override fun dump(): String {
        return "%${name()} = $NAME $predicate ${first().type()} ${first()}, ${second()}"
    }

    override fun predicate(): IntPredicate = predicate

    override fun operandsType(): UnsignedIntType {
        val opType = first().type()
        assert(opType is UnsignedIntType) {
            "should be, but opType=$opType"
        }

        return first().type() as UnsignedIntType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "ucmp"

        fun make(id: Identity, owner: Block, a: Value, predicate: IntPredicate, b: Value): UnsignedIntCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "inconsistent type in '$id', but a=$a:$aType, b=$b:$bType"
            }

            return registerUser(UnsignedIntCompare(id, owner, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && aType is UnsignedIntType
        }

        fun typeCheck(icmp: UnsignedIntCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}