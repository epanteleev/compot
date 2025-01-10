package ir.instruction

import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class IntCompare private constructor(id: Identity, owner: Block, operandsType: PrimitiveType, a: Value, private val predicate: IntPredicate, b: Value) :
    CompareInstruction(id, owner, operandsType, a, b) {
    override fun dump(): String {
        return "%${name()} = $NAME $predicate $operandsType ${first()}, ${second()}"
    }

    override fun predicate(): IntPredicate = predicate

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "icmp"
        const val FIRST = 0
        const val SECOND = 1

        fun icmp(a: Value, predicate: IntPredicate, b: Value): InstBuilder<IntCompare> = { id: Identity, owner: Block ->
            make(id, owner, a, predicate, b)
        }

        private fun make(id: Identity, owner: Block, a: Value, predicate: IntPredicate, b: Value): IntCompare {
            val aType = a.type()
            val bType = b.type()
            require(isAppropriateType(aType, bType)) {
                "should be the same integer or pointer types in '$id', but a=$a:$aType, b=$b:$bType"
            }

            return registerUser(IntCompare(id, owner, aType.asType(), a, predicate, b), a, b)
        }

        private fun isAppropriateType(aType: Type, bType: Type): Boolean {
            return aType == bType && (aType is IntegerType || aType is PtrType)
        }

        fun typeCheck(icmp: IntCompare): Boolean {
            return isAppropriateType(icmp.first().type(), icmp.second().type())
        }
    }
}