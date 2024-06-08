package ir.instruction

import common.assertion
import ir.Value
import ir.types.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Load private constructor(id: Identity, owner: Block, loadedType: PrimitiveType, ptr: Value):
    ValueInstruction(id, owner, loadedType, arrayOf(ptr)) {
    override fun dump(): String {
        return "%${name()} = $NAME $tp ${operand()}"
    }

    fun operand(): Value {
        assertion(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "load"

        fun make(id: Identity, owner: Block, loadedType: PrimitiveType, operand: Value): Load {
            val type = operand.type()
            require(isAppropriateTypes(type)) {
                "inconsistent types in '$id' type=${loadedType}, but operand=${operand}:$type"
            }

            return registerUser(Load(id, owner, loadedType, operand), operand)
        }

        private fun isAppropriateTypes(tp: Type): Boolean {
            return tp is PointerType
        }

        fun typeCheck(load: Load): Boolean {
            return isAppropriateTypes(load.operand().type())
        }
    }
}