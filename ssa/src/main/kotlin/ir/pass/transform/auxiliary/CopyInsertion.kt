package ir.pass.transform.auxiliary

import ir.instruction.*
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.module.FunctionData


internal class CopyInsertion private constructor(private val cfg: FunctionData) {
    private fun isolatePhis(bb: Block, i: Int, phi: Phi): Int {
        phi.zipWithIndex { incoming, operand, idx ->
            assert(!bb.hasCriticalEdgeFrom(incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val copy = incoming.insert(incoming.size - 1) {
                it.copy(operand)
            }
            phi.update(idx, copy)
        }

        val copy = bb.insert(i + 1) { it.copy(phi) }
        ValueInstruction.replaceUsages(phi, copy)
        //assert(bb.instructions()[i] == phi) { //TODO
         //   "Expected phi instruction at $i, but got ${bb.instructions()[i]}"
       // }
        return 1
    }

    fun pass() {
        for (bb in cfg.blocks) {
            bb.forEachInstruction { i, inst ->
                val phi =  inst as? Phi ?: return@forEachInstruction 0
                isolatePhis(bb, i, phi)
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