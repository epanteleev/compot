package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.module.block.Block

class ReturnVoid private constructor(): Return(arrayOf()) {
    override fun dump(): String {
        return "ret void"
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        private val ret = ReturnVoid()

        fun make(): ReturnVoid {
            return ret
        }
    }
}