package ir.instruction.lir

import common.assertion
import ir.value.Value
import ir.types.*
import ir.instruction.Identity
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class IndexedLoad private constructor(id: Identity, owner: Block, loadedType: PrimitiveType, origin: Value, index: Value):
    ValueInstruction(id, owner, loadedType, arrayOf(origin, index)) {

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%${name()} = $NAME $tp ${origin()}, ${index().type()} ${index()}"
    }

    fun origin(): Value {
        assertion(operands.size == 2) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "indexedLoad"

        fun make(id: Identity, owner: Block, loadedType: PrimitiveType, origin: Value, index: Value): IndexedLoad {
            val originType = origin.type()
            require(isAppropriateType(originType, index.type())) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(IndexedLoad(id, owner, loadedType, origin, index), origin)
        }

        fun typeCheck(copy: IndexedLoad): Boolean {
            return isAppropriateType(copy.origin().type(), copy.index().type())
        }

        private fun isAppropriateType(originType: Type, index: Type): Boolean {
            return originType is PointerType && index is ArithmeticType
        }
    }
}