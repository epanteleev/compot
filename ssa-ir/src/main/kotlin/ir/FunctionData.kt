package ir

import ir.codegen.x64.regalloc.liveness.LiveIntervals
import ir.codegen.x64.regalloc.liveness.Liveness
import ir.utils.CopyModule

class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks) {
    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun liveness(): LiveIntervals {
        return Liveness.evaluate(this)
    }

    fun copy(): FunctionData {
        return CopyModule.copy(this)
    }

    companion object {
        fun create(prototype: FunctionPrototype, basicBlocks: BasicBlocks, argumentValues: List<ArgumentValue>): FunctionData {
            return FunctionData(prototype, argumentValues, basicBlocks)
        }
    }
}