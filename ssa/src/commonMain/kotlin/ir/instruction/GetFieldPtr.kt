package ir.instruction

import ir.types.*
import ir.value.*
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class GetFieldPtr private constructor(id: Identity, owner: Block, val basicType: AggregateType, source: Value, private val index: Array<IntegerConstant>):
    ValueInstruction(id, owner, Type.Ptr, arrayOf(source)) {
    private fun getOperand(index: Int): Value {
        assertion(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[index]
    }

    override fun dump(): String {
        val stringBuilder = StringBuilder("%${name()} = $NAME $basicType, ptr ${source()}, ")
        index.forEachIndexed { index, value ->
            stringBuilder.append("${value.type()} $value")
            if (index != this.index.size - 1) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }

    override fun type(): PointerType {
        return tp as PointerType
    }

    fun source(): Value = getOperand(0)

    fun index(number: Int): IntegerConstant = index[number]

    fun indexes(): Array<IntegerConstant> = index

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gfp"

        fun make(id: Identity, owner: Block, type: AggregateType, source: Value, indexes: Array<IntegerConstant>): GetFieldPtr {
            val sourceType = source.type()
            val indexType  = indexes[0].type()
            require(isAppropriateType(sourceType)) {
                "inconsistent types in '$id' type=$type, source=$source:$sourceType, index=${indexes[0]}:$indexType"
            }

            return registerUser(GetFieldPtr(id, owner, type, source, indexes), source)
        }

        private fun isAppropriateType(sourceType: Type): Boolean {
            return sourceType is PointerType
        }

        fun typeCheck(gep: GetFieldPtr): Boolean {
            return isAppropriateType(gep.source().type())
        }
    }
}