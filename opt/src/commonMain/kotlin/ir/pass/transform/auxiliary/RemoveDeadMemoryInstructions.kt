package ir.pass.transform.auxiliary

import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*
import ir.value.constant.UndefValue
import ir.pass.analysis.EscapeAnalysisPassFabric
import ir.pass.analysis.traverse.PreOrderFabric


internal class RemoveDeadMemoryInstructions private constructor(private val cfg: FunctionData) {
    private val escapeState = cfg.analysis(EscapeAnalysisPassFabric)

    private fun removeMemoryInstructions(bb: Block) {
        fun transform(instruction: Instruction): Instruction? {
            instruction.match(load(any())) { load: Load ->
                if (load.operand() == UndefValue) {
                    return bb.kill(load, UndefValue)
                }
                if (!escapeState.isNoEscape(load.operand())) {
                    return load
                }
                return bb.kill(load, UndefValue)
            }
            instruction.match(store(any(), any())) { store: Store ->
                if (store.pointer() == UndefValue) {
                    return bb.kill(store, UndefValue)
                }
                if (!escapeState.isNoEscape(store.pointer())) {
                    return store
                }
                return bb.kill(store, UndefValue)
            }
            instruction.match(alloc(primitive())) { alloc: Alloc ->
                if (!escapeState.isNoEscape(alloc)) {
                    return alloc
                }
                return bb.kill(alloc, UndefValue)
            }

            return instruction
        }

        bb.transform { transform(it) }
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