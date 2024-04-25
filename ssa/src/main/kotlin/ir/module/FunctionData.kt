package ir.module

import ir.ArgumentValue
import ir.FunctionPrototype
import ir.module.auxiliary.CopyCFG
import ir.module.auxiliary.DumpModule
import ir.platform.x64.regalloc.liveness.LiveIntervals
import ir.platform.x64.regalloc.liveness.LiveIntervalsBuilder


class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks) {
    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun liveness(): LiveIntervals {
        return LiveIntervalsBuilder.evaluate(this)
    }

    fun copy(): FunctionData {
        return CopyCFG.copy(this)
    }

    fun name(): String {
        return prototype.name
    }

    override fun hashCode(): Int {
        return prototype.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FunctionData
        return prototype == other.prototype
    }

    companion object {
        fun create(prototype: FunctionPrototype, basicBlocks: BasicBlocks, argumentValues: List<ArgumentValue>): FunctionData {
            return FunctionData(prototype, argumentValues, basicBlocks)
        }
    }
}