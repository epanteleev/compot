package ir.module.auxiliary

import ir.instruction.Instruction
import ir.module.FunctionData
import ir.module.FunctionPrototype
import ir.module.block.Block
import ir.pass.analysis.traverse.PreOrderFabric
import ir.value.ArgumentValue
import ir.value.Value


abstract class DumpFunctionData(protected val functionData: FunctionData) {
    protected val builder = StringBuilder()

    protected fun dumpFunctionData() {
        dumpPrototype(functionData.prototype, functionData.arguments())
        builder.append("{\n")

        for (bb in functionData.analysis(PreOrderFabric)) {
            dumpBlock(bb)
        }

        builder.append('}')
        builder.append('\n')
    }

    protected open fun dumpPrototype(prototype: FunctionPrototype, argumentValues: List<ArgumentValue>) {
        builder.append("define ${prototype.returnType()} @${prototype.name}(")
        argumentValues.joinTo(builder) { argumentValue -> "$argumentValue:${argumentValue.contentType()}" }
        builder.append(") ")
        prototype.attributes.forEach { builder.append("$it ") }
    }

    protected open fun dumpBlock(bb: Block) {
        val predecessors = bb.predecessors().sortedBy { it.index }
        builder.append("$bb:\t")
        if (predecessors.isNotEmpty()) {
            builder.append("; pred=")
            predecessors.joinTo(builder)
        }
        builder.append('\n')
        for ((idx, instruction) in bb.withIndex()) {
            dumpInstruction(instruction, idx)
            builder.append('\n')
        }
    }

    protected open fun dumpInstruction(instruction: Instruction, idx: Int) {
        builder.append('\t')
        builder.append(instruction.dump())
    }

    fun builder(): StringBuilder {
        dumpFunctionData()
        return builder
    }

    override fun toString(): String {
        return builder().toString()
    }
}

class DumpSSAFunctionData(functionData: FunctionData): DumpFunctionData(functionData) {

}