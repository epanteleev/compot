package ir.pass.transform.auxiliary

import ir.instruction.*
import ir.ArgumentValue
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.module.block.Label
import ir.module.FunctionData


internal class CopyInsertion private constructor(private val cfg: FunctionData) {
    private fun isolatePhis(bb: Block, i: Int, phi: Phi) {
        phi.zipWithIndex { incoming, operand, idx ->
            assert(!bb.hasCriticalEdgeFrom(incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val copy = incoming.insert(incoming.instructions().size - 1) {
                it.copy(operand)
            }
            phi.update(idx, copy)
        }

        val copy = bb.insert(i + 1) { it.copy(phi) }
        ValueInstruction.replaceUsages(phi, copy)
        assert(bb.instructions()[i] == phi) {
            "Expected phi instruction at $i, but got ${bb.instructions()[i]}"
        }
    }

    fun pass() {
        for (bb in cfg.blocks) {
            var i = -1
            while (i < bb.instructions().size - 1) {
                i += 1
                val phi =  bb.instructions()[i] as? Phi ?: continue
                isolatePhis(bb, i, phi)
                i += 1
            }
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { CopyInsertion(it).pass() }
            return SSAModule(module.functions, module.externFunctions, module.globals, module.types)
        }
    }
}