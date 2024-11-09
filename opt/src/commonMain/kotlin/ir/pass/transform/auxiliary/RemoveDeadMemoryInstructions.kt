package ir.pass.transform.auxiliary

import ir.module.*
import ir.value.Value
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*
import ir.pass.analysis.EscapeAnalysisPassFabric
import ir.pass.analysis.traverse.PreOrderFabric


class RemoveDeadMemoryInstructions private constructor(private val cfg: FunctionData) {
    private val escapeState = cfg.analysis(EscapeAnalysisPassFabric)

    private fun removeMemoryInstructions(bb: Block) {
        fun transform(instruction: Instruction): Instruction? {
            instruction.match(load(nop())) { load: Load ->
                if (load.operand() == Value.UNDEF) {
                    return bb.kill(load, Value.UNDEF)
                }
                if (!escapeState.isNoEscape(load)) {
                    return load
                }
                return bb.kill(load, Value.UNDEF)
            }
            instruction.match(store(nop(), nop())) { store: Store ->
                if (store.pointer() == Value.UNDEF) {
                    return bb.kill(store, Value.UNDEF)
                }
                if (!escapeState.isNoEscape(store.pointer())) {
                    return store
                }
                return bb.kill(store, Value.UNDEF)
            }
            instruction.match(alloc(primitive())) { alloc: Alloc ->
                if (!escapeState.isNoEscape(alloc)) {
                    return alloc
                }
                return bb.kill(alloc, Value.UNDEF)
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