package ir.instruction

import ir.Value
import ir.module.block.Block

class ReturnVoid: Return(arrayOf()) {
    override fun dump(): String {
        return "ret void"
    }

    override fun copy(usages: List<Value>, newTargets: Array<Block>): ReturnVoid {
        return this
    }

    override fun copy(newUsages: List<Value>): ReturnVoid {
        return this
    }
}