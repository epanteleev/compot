package ir.instruction

import ir.Value
import ir.types.*
import ir.IntegerConstant
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class GetFieldPtr private constructor(name: String, owner: Block, val basicType: AggregateType, source: Value, index: IntegerConstant):
    ValueInstruction(name, owner, Type.Ptr, arrayOf(source, index)) {
    override fun dump(): String {
        return "%$id = $NAME $basicType, ptr ${source()}, ${index().type()} ${index()}"
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    fun source(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun index(): IntegerConstant {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1] as IntegerConstant
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gfp"

        fun make(name: String, owner: Block, type: AggregateType, source: Value, index: IntegerConstant): GetFieldPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types in '$name' type=$type, source=$source:$sourceType, index=$index:$indexType"
            }

            return registerUser(GetFieldPtr(name, owner, type, source, index), source, index)
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