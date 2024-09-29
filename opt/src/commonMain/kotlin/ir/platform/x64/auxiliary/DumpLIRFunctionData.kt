package ir.platform.x64.auxiliary

import ir.instruction.Instruction
import ir.module.FunctionData
import ir.module.auxiliary.DumpFunctionData
import ir.module.block.Block
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.platform.x64.pass.analysis.regalloc.LinearScanFabric
import ir.value.LocalValue
import ir.value.TupleValue


class DumpLIRFunctionData(functionData: FunctionData): DumpFunctionData(functionData) {
    private val regAlloc = functionData.analysis(LinearScanFabric)
    private val liveness = functionData.analysis(LiveIntervalsFabric)
    private var currentBlock: Block? = null

    override fun dumpBlock(bb: Block) {
        currentBlock = bb
        super.dumpBlock(bb)
        killedInBlock(bb)
    }

    override fun dumpInstruction(instruction: Instruction, idx: Int) {
        if (instruction is LocalValue && instruction !is TupleValue) {
            val operand = regAlloc.operandOrNull(instruction)
            if (operand != null) {
                builder.append("[$operand]")
            }
        }

        super.dumpInstruction(instruction, idx)

        val killed = arrayListOf<LocalValue>()
        instruction.operands { use ->
            if (use !is LocalValue) {
                return@operands
            }

            val end = liveness[use].end()
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

        for (current in functionData) {
            for (inst in current) {
                if (inst !is LocalValue) {
                    continue
                }

                val end = liveness[inst].end()
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