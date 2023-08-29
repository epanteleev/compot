package ir

import ir.utils.LiveIntervals

data class FunctionPrototype(val name: String, val returnType: Type, val arguments: List<Type>) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("define $name(")
        arguments.joinTo(builder)
        builder.append("): $returnType")
        return builder.toString()
    }
}

class FunctionData(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, var blocks: BasicBlocks) {
    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun liveness(): LiveIntervals {
        return LiveIntervals.evaluate(this)
    }

    companion object {
        fun create(name: String, returnType: Type, arguments: List<Type>, argumentValues: List<ArgumentValue>): FunctionData {
            val prototype = FunctionPrototype(name, returnType, arguments)
            return FunctionData(prototype, argumentValues, BasicBlocks.create())
        }
    }
}