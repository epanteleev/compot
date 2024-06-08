package ir.instruction

import common.assertion
import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class GetElementPtr private constructor(id: Identity, owner: Block, val basicType: PrimitiveType, source: Value, index: Value):
    ValueInstruction(id, owner, Type.Ptr, arrayOf(source, index)) {
    override fun dump(): String {
        return "%${name()} = $NAME $basicType, ptr ${source()}, ${index().type()} ${index()}"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    fun source(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
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
        const val NAME = "gep"

        fun make(id: Identity, owner: Block, elementType: PrimitiveType, source: Value, index: Value): GetElementPtr {
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
            if (sourceType !is PointerType) {
                return false
            }

            return true
        }

        fun typeCheck(gep: GetElementPtr): Boolean {
            return isAppropriateType(gep.source().type(), gep.index().type())
        }
    }
}