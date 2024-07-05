package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.*
import ir.value.IntegerConstant
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class GetFieldPtr private constructor(id: Identity, owner: Block, val basicType: AggregateType, source: Value, index: IntegerConstant):
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

    fun index(): IntegerConstant {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1] as IntegerConstant
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gfp"

        fun make(id: Identity, owner: Block, type: AggregateType, source: Value, index: IntegerConstant): GetFieldPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types in '$id' type=$type, source=$source:$sourceType, index=$index:$indexType"
            }

            return registerUser(GetFieldPtr(id, owner, type, source, index), source, index)
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

        fun typeCheck(gep: GetFieldPtr): Boolean {
            return isAppropriateType(gep.source().type(), gep.index().type())
        }
    }
}