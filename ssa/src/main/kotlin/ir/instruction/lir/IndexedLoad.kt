package ir.instruction.lir

import ir.Value
import ir.types.Type
import ir.types.PrimitiveType
import ir.types.ArithmeticType
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor
import ir.types.PointerType


class IndexedLoad private constructor(name: String, loadedType: PrimitiveType, origin: Value, index: Value):
    ValueInstruction(name, loadedType, arrayOf(origin, index)) {

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun dump(): String {
        return "%$identifier = $NAME $tp ${origin()}, ${index().type()} ${index()}"
    }

    fun origin(): Value {
        assert(operands.size == 2) {
            "size should be 1 in $this instruction"
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
        const val NAME = "indexedLoad"

        fun make(name: String, loadedType: PrimitiveType, origin: Value, index: Value): IndexedLoad {
            val originType = origin.type()
            require(isAppropriateType(originType, index.type())) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(IndexedLoad(name, loadedType, origin, index), origin)
        }

        fun typeCheck(copy: IndexedLoad): Boolean {
            return isAppropriateType(copy.origin().type(), copy.index().type())
        }

        private fun isAppropriateType(originType: Type, index: Type): Boolean {
            return originType is PointerType && index is ArithmeticType
        }
    }
}