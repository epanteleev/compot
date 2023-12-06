package ir.module.auxiliary

import ir.FunctionPrototype
import ir.LocalValue
import ir.Value
import ir.instruction.Instruction
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.platform.liveness.LiveIntervals
import ir.platform.regalloc.RegisterAllocation
import ir.platform.x64.CSSAModule

abstract class DumpModule<T: Module> protected constructor(protected val module: T) {
    protected val builder = StringBuilder()

    private fun dump() {
        dumpExternFunctions()
        dumpConstants()
        for (fn in module.functions()) {
            dumpFunctionData(fn)
        }
    }

    private fun dumpExternFunctions() {
        for (fn in module.externFunctions) {
            builder.append(fn)
            builder.append('\n')
        }
        builder.append('\n')
    }

    private fun dumpConstants() {
        for (c in module.constants) {
            builder.append(c.dump())
            builder.append('\n')
        }
        builder.append('\n')
    }

    protected open fun dumpFunctionData(functionData: FunctionData) {
        dumpPrototype(functionData.prototype, functionData.arguments())
        builder.append("{\n")

        for (bb in functionData.blocks.preorder()) {
            dumpBlock(bb)
        }

        builder.append('}')
        builder.append('\n')
    }

    protected open fun dumpBlock(bb: Block) {
        val predecessors = bb.predecessors().sortedBy { it.index }
        builder.append("$bb:\t")
        if (predecessors.isNotEmpty()) {
            builder.append("; pred=")
            predecessors.joinTo(builder)
        }
        builder.append('\n')
        for ((idx, instruction) in bb.instructions().withIndex()) {
            dumpInstruction(instruction, idx)
            builder.append('\n')
        }
    }

    protected open fun dumpPrototype(prototype: FunctionPrototype, argumentValues: List<Value>) {
        builder.append("define ${prototype.type()} ${prototype.name}(")
        argumentValues.joinTo(builder) { argumentValue -> "$argumentValue:${argumentValue.type()}" }
        builder.append(") ")
    }

    protected open fun dumpInstruction(instruction: Instruction, idx: Int) {
        builder.append('\t')
        builder.append(instruction.dump())
    }

    override fun toString(): String {
        dump()
        return builder.toString()
    }

    companion object {
        fun dump(module: Module): String {
            val dump = when (module) {
                is SSAModule  -> DumpSSAlModule(module)
                is CSSAModule -> DumpCSSAModule(module)
                else -> throw RuntimeException("undefined")
            }

            return dump.toString()
        }
    }
}

private class DumpSSAlModule(module: SSAModule) : DumpModule<SSAModule>(module)

private class DumpCSSAModule(module: CSSAModule) : DumpModule<CSSAModule>(module) {
    private var regAlloc: RegisterAllocation? = null
    private var liveness: LiveIntervals? = null
    private var currentBlock: Block? = null

    override fun dumpFunctionData(functionData: FunctionData) {
        regAlloc = module.regAlloc(functionData)
        liveness = module.liveInfo(functionData)
        super.dumpFunctionData(functionData)
    }

    override fun dumpBlock(bb: Block) {
        currentBlock = bb
        super.dumpBlock(bb)
    }

    override fun dumpInstruction(instruction: Instruction, idx: Int) {
        if (instruction is LocalValue) {
            val operand = regAlloc!!.operand(instruction)
            builder.append("[$operand]")
        }

        super.dumpInstruction(instruction, idx)

        val killed = arrayListOf<LocalValue>()
        for (use in instruction.operands()) {
            if (use !is LocalValue) {
                continue
            }

            val end = liveness!![use].end()
            if (end.thisPlace(currentBlock!!, idx)) {
                killed.add(use)
            }
        }

        if (killed.isNotEmpty()) {
            builder.append("\tkill: ")
            killed.joinTo(builder, separator = ",") { it.toString() }
        }
    }
}