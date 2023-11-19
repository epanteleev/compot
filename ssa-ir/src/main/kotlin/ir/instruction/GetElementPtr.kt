package ir.instruction

import ir.*
import ir.instruction.utils.Visitor
import ir.types.*

class GetElementPtr private constructor(name: String, tp: Type, source: Value, index: Value):
    ValueInstruction(name, tp, arrayOf(source, index)) {
    override fun dump(): String {
        return "%$identifier = gep $tp ${source()}, ${index().type()} ${index()}"
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

    override fun copy(newUsages: List<Value>): GetElementPtr {
        assert(newUsages.size == 2) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, type(), newUsages[0], newUsages[1])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, source: Value, index: Value): GetElementPtr {
            return make(name, gepTypeFrom((source.type() as PointerType).type), source, index)
        }

        fun make(name: String, tp: Type, source: Value, index: Value): GetElementPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types source.type=$sourceType, index.type=$indexType"
            }

            if (index is ValueInstruction) {
                require(indexType == Type.I64 || indexType == Type.U64) {
                    "$index should be ${Type.I64} or ${Type.U64}"
                }
            }

            return registerUser(GetElementPtr(name, tp, source, index), source, index)
        }

        private fun gepTypeFrom(sourceType: Type): Type {
            return when (sourceType) {
                is ArrayType     -> sourceType.elementType().ptr()
                is PrimitiveType -> sourceType.ptr()
                else -> { throw RuntimeException("sourceType=$sourceType") }
            }
        }

        private fun isAppropriateType(sourceType: Type, indexType: Type): Boolean {
            if (indexType !is ArithmeticType) {
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