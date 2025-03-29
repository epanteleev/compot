package ir.platform.x64.auxiliary

import ir.instruction.Instruction
import ir.module.FunctionData
import ir.module.auxiliary.DumpFunctionData
import ir.module.block.Block
import ir.platform.x64.pass.analysis.regalloc.LinearScanFabric
import ir.value.LocalValue
import ir.value.TupleValue


class DumpLIRFunctionData(functionData: FunctionData): DumpFunctionData(functionData) {
    private val regAlloc = functionData.analysis(LinearScanFabric)
    private var currentBlock: Block? = null

    override fun dumpBlock(bb: Block) {
        currentBlock = bb
        super.dumpBlock(bb)
    }

    override fun dumpInstruction(instruction: Instruction, idx: Int) {
        if (instruction is LocalValue && instruction !is TupleValue) {
            val operand = regAlloc.operandOrNull(instruction)
            if (operand != null) {
                builder.append("[$operand]")
            }
        }

        super.dumpInstruction(instruction, idx)
    }
}