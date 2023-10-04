package ir.pass.transform

import ir.*

class CopyInsertion private constructor(private val cfg: BasicBlocks) {
    private var index = cfg.maxInstructionIndex()

    private fun hasCriticalEdge(bb: BasicBlock, predecessor: BasicBlock): Boolean {
        return predecessor.successors().size > 1 && bb.predecessors().size > 1
    }

    private fun modifyPhis(bb: BasicBlock, phi: Phi) {
        val newValues = hashMapOf<Value, Value>()
        for ((incoming, operand) in phi.zip()) {
            index += 1
            assert(!hasCriticalEdge(bb, incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val copy = Copy(index, operand)
            newValues[operand] = copy

            if (bb == incoming) {
                bb.appendBefore(copy, phi)
            } else {
                incoming.appendBeforeTerminateInstruction(copy)
            }
        }

        phi.updateUsagesInPhi { v, _ -> newValues[v]!! }
    }

    fun pass() {
        for (bb in cfg) {
            bb.phis().forEach { modifyPhis(bb, it) }
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { CopyInsertion(it.blocks).pass() }
            return module
        }
    }
}