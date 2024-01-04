package ir.instruction

import ir.Value
import ir.types.*
import ir.IntegerConstant
import ir.instruction.utils.Visitor


class GetFieldPtr private constructor(name: String, val basicType: AggregateType, source: Value, index: IntegerConstant):
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

    override fun copy(newUsages: List<Value>): GetFieldPtr {
        assert(newUsages.size == 2) {
            "should be, but newUsages=$newUsages"
        }
        val constant = newUsages[1]
        assert(constant is IntegerConstant) {
            "should be, but constant=$constant"
        }
        return make(identifier, basicType, newUsages[0], constant as IntegerConstant)
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        const val NAME = "gfp"

        fun make(name: String, tp: AggregateType, source: Value, index: IntegerConstant): GetFieldPtr {
            val sourceType = source.type()
            val indexType  = index.type()
            require(isAppropriateType(sourceType, indexType)) {
                "inconsistent types source.type=$sourceType, index.type=$indexType"
            }

            return registerUser(GetFieldPtr(name, tp, source, index), source, index)
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

        fun isCorrect(gep: GetFieldPtr): Boolean {
            return isAppropriateType(gep.source().type(), gep.index().type())
        }
    }
}