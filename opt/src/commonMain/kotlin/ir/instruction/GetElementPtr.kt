package ir.instruction

import common.assertion
import ir.Definitions.QWORD_SIZE
import ir.value.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class GetElementPtr private constructor(id: Identity, owner: Block, val basicType: NonTrivialType, source: Value, index: Value):
    AnyGetElementPtr(id, owner, arrayOf(source, index)) {
    override fun dump(): String {
        return "%${name()} = $NAME $basicType, ptr ${source()}, ${index().type()} ${index()}"
    }

    override fun type(): PtrType = PtrType
    override fun accessType(): NonTrivialType = basicType

    override fun index(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[INDEX]
    }

    fun index(newIndex: Value) {
        update(INDEX, newIndex)
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        private const val SOURCE = 0
        private const val INDEX  = 1
        const val NAME = "gep"

        fun gep(source: Value, elementType: NonTrivialType, index: Value): InstBuilder<GetElementPtr> = {
            id: Identity, owner: Block -> make(id, owner, elementType, source, index)
        }

        private fun make(id: Identity, owner: Block, elementType: NonTrivialType, source: Value, index: Value): GetElementPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types in '$id' type=$elementType source=$source:$sourceType, index=$index:$indexType"
            }

            return registerUser(GetElementPtr(id, owner, elementType, source, index), source, index)
        }

        private fun isAppropriateType(sourceType: Type, indexType: Type): Boolean {
            if (indexType !is IntegerType) {
                return false
            }
            if (indexType.sizeOf() != QWORD_SIZE) {
                return false
            }
            if (sourceType !is PtrType) {
                return false
            }

            return true
        }

        fun typeCheck(gep: GetElementPtr): Boolean {
            return isAppropriateType(gep.source().type(), gep.index().type())
        }
    }
}