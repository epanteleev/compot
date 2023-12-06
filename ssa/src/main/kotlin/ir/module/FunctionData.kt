package ir.module

import ir.ArgumentValue
import ir.FunctionPrototype
import ir.GlobalSymbol
import ir.module.auxiliary.Copy
import ir.platform.liveness.LiveIntervals
import ir.platform.liveness.Liveness

class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks):
    GlobalSymbol {
    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun liveness(): LiveIntervals {
        return Liveness.evaluate(this)
    }

    fun copy(): FunctionData {
        return Copy.copy(this)
    }

    override fun name(): String {
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