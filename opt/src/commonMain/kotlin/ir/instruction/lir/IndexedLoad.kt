package ir.instruction.lir

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.Identity
import ir.Definitions.QWORD_SIZE
import ir.instruction.InstBuilder
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor


class IndexedLoad private constructor(id: Identity, owner: Block, private val loadedType: PrimitiveType, origin: Value, index: Value):
    ValueInstruction(id, owner, arrayOf(origin, index)) {
    private fun checkedGet(index: Int): Value {
        assertion(operands.size == 2) {
            "size should be 1 in $this instruction"
        }

        return operands[index]
    }

    override fun type(): PrimitiveType = loadedType

    override fun dump(): String {
        return "%${name()} = $NAME $loadedType ${origin()}, ${index().type()} ${index()}"
    }

    fun origin(): Value = checkedGet(0)

    fun index(): Value = checkedGet(1)

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "indexedLoad"

        fun load(origin: Value, loadedType: PrimitiveType, index: Value): InstBuilder<IndexedLoad> = {
            id: Identity, owner: Block -> make(id, owner, loadedType, origin, index)
        }

        private fun make(id: Identity, owner: Block, loadedType: PrimitiveType, origin: Value, index: Value): IndexedLoad {
            val originType = origin.type()
            require(isAppropriateType(origin, index)) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(IndexedLoad(id, owner, loadedType, origin, index), origin)
        }

        fun typeCheck(copy: IndexedLoad): Boolean {
            return isAppropriateType(copy.origin(), copy.index())
        }

        private fun isAppropriateType(origin: Value, index: Value): Boolean {
            if (origin.type() !is PtrType) {
                return false
            }
            val idxType = index.type()
            if (idxType !is ArithmeticType) {
                return false
            }

            return idxType.sizeOf() == QWORD_SIZE
        }
    }
}