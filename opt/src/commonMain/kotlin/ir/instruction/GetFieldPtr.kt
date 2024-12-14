package ir.instruction

import ir.types.*
import ir.value.*
import common.assertion
import ir.module.block.Block
import ir.Definitions.QWORD_SIZE
import ir.value.constant.IntegerConstant
import ir.instruction.utils.IRInstructionVisitor


class GetFieldPtr private constructor(id: Identity, owner: Block, val basicType: AggregateType, source: Value, private val index: IntegerConstant):
    ValueInstruction(id, owner, arrayOf(source)) {
    override fun dump(): String {
        val stringBuilder = StringBuilder("%${name()} = $NAME $basicType, ptr ${source()}, ")
        stringBuilder.append("${index.type()} $index")
        return stringBuilder.toString()
    }

    override fun type(): PtrType = PtrType

    fun source(): Value {
        assertion(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[SOURCE]
    }

    fun index(): IntegerConstant = index

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val SOURCE = 0
        const val NAME = "gfp"

        fun gfp(type: AggregateType, source: Value, index: IntegerConstant): InstBuilder<GetFieldPtr> = {
            id: Identity, owner: Block -> make(id, owner, type, source, index)
        }

        private fun make(id: Identity, owner: Block, type: AggregateType, source: Value, index: IntegerConstant): GetFieldPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, index)) {
                "inconsistent types in '$id' type=$type, source=$source:$sourceType, index=$index:$indexType"
            }

            return registerUser(GetFieldPtr(id, owner, type, source, index), source)
        }

        private fun isAppropriateType(sourceType: Type, index: IntegerConstant): Boolean {
            if (index.type().sizeOf() != QWORD_SIZE) {
                return false
            }

            return sourceType is PtrType || sourceType is AggregateType
        }

        fun typeCheck(gep: GetFieldPtr): Boolean {
            return isAppropriateType(gep.source().type(), gep.index())
        }
    }
}