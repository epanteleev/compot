package ir.module

import ir.ArgumentValue
import ir.FunctionPrototype
import ir.platform.liveness.LiveIntervals
import ir.platform.liveness.Liveness
import ir.module.auxiliary.Copy

class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks) {
    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun liveness(): LiveIntervals {
        return Liveness.evaluate(this)
    }

    fun copy(): FunctionData {
        return Copy.copy(this)
    }

    companion object {
        fun create(prototype: FunctionPrototype, basicBlocks: BasicBlocks, argumentValues: List<ArgumentValue>): FunctionData {
            return FunctionData(prototype, argumentValues, basicBlocks)
        }
    }
}