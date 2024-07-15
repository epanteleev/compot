package ir.instruction.lir

import ir.types.*
import ir.value.Value
import common.assertion
import ir.instruction.Identity
import ir.Definitions.WORD_SIZE
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.value.asType


class IndexedLoad private constructor(id: Identity, owner: Block, loadedType: PrimitiveType, origin: Value, index: Value):
    ValueInstruction(id, owner, loadedType, arrayOf(origin, index)) {
    private inline fun checkedGet(index: Int): Value {
        assertion(operands.size == 2) {
            "size should be 1 in $this instruction"
        }

        return operands[index]
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%${name()} = $NAME $tp ${origin()}, ${index().type()} ${index()}"
    }

    fun origin(): Value = checkedGet(0)

    fun index(): Value = checkedGet(1)

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "indexedLoad"

        fun make(id: Identity, owner: Block, loadedType: PrimitiveType, origin: Value, index: Value): IndexedLoad {
            val originType = origin.type()
            require(isAppropriateType(loadedType, origin, index)) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(IndexedLoad(id, owner, loadedType, origin, index), origin)
        }

        fun typeCheck(copy: IndexedLoad): Boolean {
            return isAppropriateType(copy.type(), copy.origin(), copy.index())
        }

        private fun isAppropriateType(loadedType: Type, origin: Value, index: Value): Boolean {
            if (origin.type() !is PointerType) {
                return false
            }
            if (loadedType !is PrimitiveType) {
                return false
            }
            if (index.type() !is ArithmeticType) {
                return false
            }

            return index.asType<NonTrivialType>().sizeOf() >= WORD_SIZE
        }
    }
}