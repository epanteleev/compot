package ir.module

import ir.value.ArgumentValue
import ir.module.auxiliary.CopyCFG
import ir.liveness.LiveIntervals
import ir.liveness.LiveIntervalsBuilder
import ir.types.Type


class FunctionData private constructor(name: String,
                                       returnType: Type,
                                       arguments: List<Type>,
                                       isVararg: Boolean,
                                       private var argumentValues: List<ArgumentValue>,
                                       val blocks: BasicBlocks):
    AnyFunctionPrototype(name, returnType, arguments, isVararg) {

    fun argumentValues(): List<ArgumentValue> {
        return argumentValues
    }

    fun liveness(): LiveIntervals {
        return LiveIntervalsBuilder.evaluate(this)
    }

    fun copy(): FunctionData {
        return CopyCFG.copy(this)
    }

    fun name(): String {
        return name
    }

    companion object {
        fun create(name: String,
                   returnType: Type,
                   arguments: List<Type>,
                   isVararg: Boolean, basicBlocks: BasicBlocks, argumentValues: List<ArgumentValue>): FunctionData {
            return FunctionData(name, returnType, arguments, isVararg, argumentValues, basicBlocks)
        }
    }
}