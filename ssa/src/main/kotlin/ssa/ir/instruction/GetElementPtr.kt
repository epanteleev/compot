package ir.instruction

import ir.*
import ir.instruction.utils.Visitor
import ir.types.*

class GetElementPtr private constructor(name: String, val basicType: PrimitiveType, source: Value, index: Value):
    ValueInstruction(name, Type.Ptr, arrayOf(source, index)) {
    override fun dump(): String {
        return "%$identifier = gep $basicType, ptr ${source()}, ${index().type()} ${index()}"
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

        return make(identifier, basicType, newUsages[0], newUsages[1])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {

        fun make(name: String, tp: PrimitiveType, source: Value, index: Value): GetElementPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types source.type=$sourceType, index.type=$indexType"
            }

            return registerUser(GetElementPtr(name, tp, source, index), source, index)
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