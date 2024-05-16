package ir.instruction

import ir.instruction.utils.IRInstructionVisitor


class ReturnVoid private constructor(): Return(arrayOf()) {
    override fun dump(): String = "ret void"

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(): ReturnVoid = ReturnVoid()
    }
}