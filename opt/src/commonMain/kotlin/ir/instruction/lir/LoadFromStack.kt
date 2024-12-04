package ir.instruction.lir

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.Identity
import ir.Definitions.QWORD_SIZE
import ir.instruction.ValueInstruction
import ir.instruction.utils.IRInstructionVisitor


class LoadFromStack private constructor(id: Identity, owner: Block, private val loadedType: PrimitiveType, origin: Value, index: Value):
    ValueInstruction(id, owner, arrayOf(origin, index)) {

    override fun type(): PrimitiveType = loadedType

    override fun dump(): String {
        return "%${name()} = $NAME $loadedType ${origin()}, ${index().type()} ${index()}"
    }

    fun origin(): Value {
        assertion(operands.size == 2) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    fun index(): Value {
        assertion(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "loadst"

        fun make(id: Identity, owner: Block, loadedType: PrimitiveType, origin: Value, index: Value): LoadFromStack {
            val originType = origin.type()
            require(isAppropriateType(originType, index.type())) {
                "should not be $originType, but origin=$origin:$originType"
            }

            return registerUser(LoadFromStack(id, owner, loadedType, origin, index), origin)
        }

        fun typeCheck(copy: LoadFromStack): Boolean {
            return isAppropriateType(copy.origin().type(), copy.index().type())
        }

        private fun isAppropriateType(originType: Type, index: Type): Boolean {
            if (index !is ArithmeticType) {
                return false
            }

            return index.sizeOf() == QWORD_SIZE && (originType is PointerType || originType is AggregateType)
        }
    }
}