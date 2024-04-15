package ir.instruction

import ir.instruction.utils.Visitor


class ReturnVoid private constructor(): Return(arrayOf()) {
    override fun dump(): String = "ret void"

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        private val ret = ReturnVoid()

        fun make(): ReturnVoid = ret
    }
}