package ir.pass.transform.auxiliary

import ir.LocalValue
import ir.instruction.*
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.module.FunctionData


internal class CopyInsertion private constructor(private val cfg: FunctionData) {
    private fun isolatePhis(bb: Block, phi: Instruction): Instruction {
        if (phi !is Phi) {
            return phi
        }

        phi.zipWithIndex { incoming, operand, idx ->
            assert(!bb.hasCriticalEdgeFrom(incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val copy = incoming.insertBefore(incoming.last()) {
                it.copy(operand)
            }
            phi.update(idx, copy)
        }

        val copy = bb.insertAfter(phi) { it.copy(phi) }
        LocalValue.replaceUsages(phi, copy)
        return copy
    }

    fun pass() {
        for (bb in cfg.blocks) {
            bb.transform { phi -> isolatePhis(bb, phi) }
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { CopyInsertion(it).pass() }
            return SSAModule(module.functions, module.externFunctions, module.globals, module.types)
        }
    }
}