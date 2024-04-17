package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor


class GetElementPtr private constructor(name: String, val basicType: PrimitiveType, source: Value, index: Value):
    ValueInstruction(name, Type.Ptr, arrayOf(source, index)) {
    override fun dump(): String {
        return "%$identifier = $NAME $basicType, ptr ${source()}, ${index().type()} ${index()}"
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

    fun index(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "gep"

        fun make(name: String, type: PrimitiveType, source: Value, index: Value): GetElementPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types in '$name' type=$type source=$source:$sourceType, index=$index:$indexType"
            }

            return registerUser(GetElementPtr(name, type, source, index), source, index)
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

        fun isCorrect(gep: GetElementPtr): Boolean {
            return isAppropriateType(gep.source().type(), gep.index().type())
        }
    }
}