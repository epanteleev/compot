package ir.utils

import ir.*
import java.lang.StringBuilder
import ir.Module

class DumpModule private constructor(private val builder: StringBuilder) {
    private fun dump(module: Module) {
        for (fn in module.externFunctions) {
            builder.append(fn)
            builder.append('\n')
        }

        for (fn in module.functions()) {
            dumpFunctionData(fn)
        }
    }

    private fun dumpFunctionData(functionData: FunctionData) {
        dumpPrototype(functionData.prototype, functionData.arguments())
        builder.append("{\n")

        for (bb in functionData.blocks.preorder()) {
            val pred = bb.predecessors
            builder.append("$bb:\t")
            if (pred.isNotEmpty()) {
                builder.append("pred=")
                pred.joinTo(builder)
            }
            builder.append('\n')
            bb.instructions.joinTo(builder, separator="\n") { "\t${it.dump()}" }
            builder.append('\n')
        }

        builder.append('}')
    }

    private fun dumpPrototype(prototype: FunctionPrototype, argumentValues: List<Value>) {
        builder.append("define ${prototype.type()} ${prototype.name}(")
        argumentValues.joinTo(builder) { argumentValue -> "$argumentValue:${argumentValue.type()}" }
        builder.append(") ")
    }

    override fun toString(): String {
        return builder.toString()
    }

    companion object {
        fun apply(module: Module): String {
            val dump = DumpModule(StringBuilder())
            dump.dump(module)
            return dump.toString()
        }
    }
}