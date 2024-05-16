package ir.pass.transform

import ir.*
import ir.dominance.DominatorTree
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.Module
import ir.module.block.Block
import ir.pass.PassFabric
import ir.pass.TransformPass
import ir.pass.transform.utils.*
import ir.pass.transform.auxiliary.RemoveDeadMemoryInstructions
import ir.types.PrimitiveType
import ir.types.Type
import ir.utils.DefUseInfo


data class Mem2RegException(override val message: String): Exception(message)

class Mem2Reg internal constructor(module: Module): TransformPass(module) {
    override fun name(): String = "mem2reg"
    override fun run(): Module {
        module.functions.forEach { fnData ->
            val cfg = fnData.blocks

            val dominatorTree = cfg.dominatorTree()
            val joinSet = JoinPointSet.evaluate(cfg, dominatorTree)
            Mem2RegImpl(cfg, joinSet).pass(dominatorTree)
        }
        return PhiFunctionPruning.run(RemoveDeadMemoryInstructions.run(module))
    }
}

object Mem2RegFabric: PassFabric {
    override fun create(module: Module): TransformPass {
        return Mem2Reg(module.copy())
    }
}

private class Mem2RegImpl(private val cfg: BasicBlocks, private val joinSet: JoinPointSet) {
    private fun insertPhis() {
        for ((bb, vSet) in joinSet) {
            for (v in vSet) {
                bb.insert(0) {
                    it.uncompletedPhi(v.allocatedType as PrimitiveType, v)
                }
            }
        }
    }

    private fun completePhis(bbToMapValues: ReachingDefinition, bb: Block) {
        fun renameValues(newUsages: MutableList<Value>, block: Block, v: Value, expectedType: Type) {
            val newValue = when (v) {
                is ValueInstruction -> bbToMapValues.rename(block, v, expectedType)
                else -> v
            }
            newUsages.add(newValue)
        }

        bb.phis { phi ->
            val newUsages = arrayListOf<Value>()
            phi.zip { l, v -> renameValues(newUsages, l, v, phi.type()) }
            phi.update(newUsages)
        }
    }

    private fun swapDependentPhis(bb: Block) {
        bb.phis { phi ->
            // Exchange phi functions
            for (used in phi.operands()) {
                if (used !is Phi) {
                    continue
                }
                if (!bb.contains(used)) {
                    continue
                }

                val usedIdx = bb.indexOf(used)
                val phiIndex = bb.indexOf(phi)

                if (usedIdx < phiIndex) {
                    bb.swap(usedIdx, phiIndex)
                }
            }
        }
    }

    private fun removeRedundantPhis(defUseInfo: DefUseInfo, deadPool: MutableSet<Instruction>, bb: Block) {
        fun filter(instruction: Instruction): Boolean {
            if (instruction !is Phi) {
                return false
            }

            if (defUseInfo.isNotUsed(instruction) || deadPool.containsAll(defUseInfo.usedIn(instruction))) {
                deadPool.add(instruction)
                return true
            }

            return false
        }

        bb.removeIf { filter(it) }
    }

    fun pass(dominatorTree: DominatorTree) {
        insertPhis()

        val bbToMapValues = ReachingDefinitionAnalysis.run(cfg, dominatorTree)
        for (bb in cfg) {
            completePhis(bbToMapValues, bb)
            swapDependentPhis(bb)
        }

        val defUseInfo = cfg.defUseInfo()
        val deadPool = hashSetOf<Instruction>()
        for (bb in cfg.postorder()) {
            removeRedundantPhis(defUseInfo, deadPool, bb)
        }
    }
}