package ir.pass.transform.auxiliary

import ir.*
import ir.block.Block
import ir.instruction.Branch
import ir.instruction.BranchCond
import ir.instruction.Phi

internal class SplitCriticalEdge private constructor(private val cfg: BasicBlocks) {
    private var maxIndex = cfg.maxBlockIndex()
    private val predecessorMap = hashMapOf<Block, Block>()

    fun pass() {
        val basicBlocks = cfg.blocks()

        for (bbIdx in 0 until basicBlocks.size) {
            val predecessors = basicBlocks[bbIdx].predecessors()
            for (index in predecessors.indices) {
                if (!basicBlocks[bbIdx].hasCriticalEdgeFrom(predecessors[index])) {
                    continue
                }

                insertBasicBlock(basicBlocks[bbIdx], predecessors[index])
            }
        }

        updatePhi(cfg)
    }

    private fun updatePhi(basicBlocks: BasicBlocks) {
        for (bb in basicBlocks) {
            bb.phis { phi ->
                var changed = false
                val validIncoming = phi.incoming().map {
                    val p = predecessorMap[it]
                    if (p != null) {
                        changed = true
                        p
                    } else {
                        it
                    }
                }

                if (!changed) {
                    return@phis
                }

                bb.update(phi) {
                    it as Phi
                    it.copy(it.usages(), validIncoming)
                }
            }
        }
    }

    private fun insertBasicBlock(bb: Block, p: Block) {
        maxIndex += 1
        val newBlock = Block.empty(maxIndex).apply {
            branch(bb)
        }

        when (val flow = p.last()) {
            is Branch -> p.updateFlowInstruction(Branch(newBlock))
            is BranchCond -> {
                val newFlowInst = when (bb) {
                    flow.onTrue() -> {
                        BranchCond(flow.condition(), newBlock, flow.onFalse())
                    }
                    flow.onFalse() -> {
                        BranchCond(flow.condition(), flow.onTrue(), newBlock)
                    }
                    else -> {
                        throw RuntimeException("internal error: p=$p, bb=$bb")
                    }
                }
                p.updateFlowInstruction(newFlowInst)
            }
            else -> {
                throw RuntimeException("unsupported terminate instruction: inst=$flow p=$p, bb=$bb")
            }
        }

        predecessorMap[p] = newBlock
        p.updateSuccessor(bb, newBlock)
        bb.removePredecessors(p)

        cfg.putBlock(newBlock)
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { fnData ->
                val cfg = fnData.blocks
                SplitCriticalEdge(cfg).pass()
            }

            return module
        }
    }
}