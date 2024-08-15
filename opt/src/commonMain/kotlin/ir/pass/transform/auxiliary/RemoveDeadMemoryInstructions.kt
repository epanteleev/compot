package ir.pass.transform.auxiliary

import ir.module.*
import ir.value.Value
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*
import ir.pass.analysis.EscapeState
import ir.pass.analysis.EscapeAnalysisPassFabric
import ir.pass.analysis.traverse.PreOrderFabric


class RemoveDeadMemoryInstructions private constructor(private val cfg: FunctionData) {
    private val escapeState = cfg.analysis(EscapeAnalysisPassFabric)

    private fun removeMemoryInstructions(bb: Block) {
        fun filter(instruction: Instruction): Instruction? {
            when {
                load(nop()) (instruction) -> { instruction as Load
                    if (instruction.operand() == Value.UNDEF) {
                        return bb.kill(instruction, Value.UNDEF)
                    }
                    if (escapeState.getEscapeState(instruction.operand()) != EscapeState.NoEscape) {
                        return instruction
                    }
                    return bb.kill(instruction, Value.UNDEF)
                }
                store(nop(), nop()) (instruction) -> { instruction as Store
                    if (instruction.pointer() == Value.UNDEF) {
                        return bb.kill(instruction, Value.UNDEF)
                    }
                    if (escapeState.getEscapeState(instruction.pointer()) != EscapeState.NoEscape) {
                        return instruction
                    }
                    return bb.kill(instruction, Value.UNDEF)
                }
                alloc(primitive()) (instruction) -> { instruction as Alloc
                    if (escapeState.getEscapeState(instruction) != EscapeState.NoEscape) {
                        return instruction
                    }
                    return bb.kill(instruction, Value.UNDEF)
                }
                else -> return instruction
            }
        }

        bb.transform { filter(it) }
    }

    fun pass() {
        for (bb in cfg.analysis(PreOrderFabric)) {
            removeMemoryInstructions(bb)
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions().forEach { fnData ->
                RemoveDeadMemoryInstructions(fnData).pass()
            }
            return module
        }
    }
}