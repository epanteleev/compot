package ir.module.auxiliary

import ir.*
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.liveness.LiveIntervals
import ir.platform.x64.regalloc.RegisterAllocation
import ir.platform.x64.LModule
import ir.types.TupleType
import ir.value.LocalValue
import ir.value.TupleValue
import ir.value.Value


abstract class DumpModule<T: Module> protected constructor(protected val module: T) {
    protected val builder = StringBuilder()

    private fun dump() {
        dumpExternFunctions()
        dumpTypes()
        dumpConstants()
        dumpGlobals()
        for ((i, fn) in module.functions().withIndex()) {
            dumpFunctionData(fn)
            if (i != module.functions().size - 1) {
                builder.append('\n')
            }
        }
    }

    private fun dumpExternFunctions() {
        for (fn in module.externFunctions.values) {
            builder.append(fn)
            builder.append('\n')
        }
        if (module.externFunctions.isNotEmpty()) {
            builder.append('\n')
        }

    }

    private fun dumpConstants() {
        for (c in module.constantPool.values) {
            builder.append(c.dump())
            builder.append('\n')
        }
        if (module.constantPool.isNotEmpty()) {
            builder.append('\n')
        }
    }

    private fun dumpGlobals() {
        for (global in module.globals.values) {
            builder.append(global.dump())
            builder.append('\n')
        }
        if (module.globals.isNotEmpty()) {
            builder.append('\n')
        }
    }

    private fun dumpTypes() {
        for (structType in module.types.values) {
            builder.append(structType.dump())
            builder.append('\n')
        }
        if (module.types.isNotEmpty()) {
            builder.append('\n')
        }
    }

    protected open fun dumpFunctionData(functionData: FunctionData) {
        dumpPrototype(functionData, functionData.argumentValues())
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
        for ((idx, instruction) in bb.withIndex()) {
            dumpInstruction(instruction, idx)
            builder.append('\n')
        }
    }

    protected open fun dumpPrototype(prototype: AnyFunctionPrototype, argumentValues: List<Value>) {
        builder.append("define ${prototype.returnType()} @${prototype.name}(")
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
                is LModule -> DumpLModule(module)
                else -> throw RuntimeException("undefined")
            }

            return dump.toString()
        }
    }
}

private class DumpSSAlModule(module: SSAModule) : DumpModule<SSAModule>(module)

private class DumpLModule(module: LModule) : DumpModule<LModule>(module) {
    private var regAlloc: RegisterAllocation? = null
    private var liveness: LiveIntervals? = null
    private var currentBlock: Block? = null
    private var currentFunctionData: FunctionData? = null


    override fun dumpFunctionData(functionData: FunctionData) {
        currentFunctionData = functionData
        regAlloc = module.regAlloc(functionData)
        liveness = module.liveInfo(functionData)
        super.dumpFunctionData(functionData)
    }

    override fun dumpBlock(bb: Block) {
        currentBlock = bb
        super.dumpBlock(bb)
        killedInBlock(bb)
    }

    override fun dumpInstruction(instruction: Instruction, idx: Int) {
        if (instruction is LocalValue && instruction !is TupleValue) {
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
            currentBlock as Block
            if (end.thisPlace(currentBlock!!, idx)) {
                killed.add(use)
            }
        }

        if (killed.isNotEmpty()) {
            builder.append("\tkill: ")
            killed.joinTo(builder, separator = ",") { it.toString() }
        }
    }

    private fun killedInBlock(bb: Block) {
        val killedInBlock = arrayListOf<LocalValue>()

        for (current in currentFunctionData!!.blocks) {
            for (inst in current) {
                if (inst !is LocalValue) {
                    continue
                }

                val end = liveness!![inst].end()
                if (end.thisPlace(bb, bb.size)) {
                    killedInBlock.add(inst)
                }
            }
        }

        if (killedInBlock.isNotEmpty()) {
            builder.append("\t; killed: ")
            killedInBlock.joinTo(builder, separator = ", ") { it.toString() }
            builder.append('\n')
        }
    }
}