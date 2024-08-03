package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class IntCompare private constructor(id: Identity, owner: Block, a: Value, private val predicate: IntPredicate, b: Value) :
    CompareInstruction(id, owner, a, b) {
    override fun dump(): String {
        return "%${name()} = $NAME $predicate ${first().type()} ${first()}, ${second()}"
    }

    override fun predicate(): IntPredicate = predicate

    override fun operandsType(): PrimitiveType {
        val opType = first().type()
        assertion(opType is IntegerType || opType is PointerType) {
            "should be, but opType=$opType"
        }

        return opType as PrimitiveType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "icmp"

        fun make(id: Identity, owner: Block, a: Value, predicate: IntPredicate, b: Value): IntCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same integer or pointer types in '$id', but a=$a:$aType, b=$b:$bType"
            }

            return registerUser(IntCompare(id, owner, a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && (aType is IntegerType || aType is PointerType)
        }

        fun typeCheck(icmp: IntCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}