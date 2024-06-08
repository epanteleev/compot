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
    private fun insertPhis(): Int {
        var insertedPhis = 0
        for ((bb, vSet) in joinSet) {
            bb as Block
            for (v in vSet) {
                insertedPhis++
                bb.prepend { it.uncompletedPhi(v.allocatedType as PrimitiveType, v) }
            }
        }

        return insertedPhis
    }

    private fun completePhis(bbToMapValues: ReachingDefinition, bb: Block) {
        fun renameValues(newUsages: MutableList<Value>, block: Block, v: Value, expectedType: Type) {
            val newValue = bbToMapValues.tryRename(block, v, expectedType)?: Value.UNDEF
            newUsages.add(newValue)
        }

        bb.phis { phi ->
            val newUsages = arrayListOf<Value>()
            phi.zip { l, v -> renameValues(newUsages, l, v, phi.type()) }
            phi.update(newUsages)
        }
    }

    // Remove unused phis
    private fun removeRedundantPhis(deadPool: MutableSet<Instruction>, bb: Block) {
        fun filter(instruction: Instruction): Boolean {
            if (instruction !is Phi) {
                return false
            }

            if (instruction.usedIn().isEmpty() || deadPool.containsAll(instruction.usedIn())) {
                deadPool.add(instruction)
                return true
            }

            return false
        }

        bb.removeIf { filter(it) }
    }

    fun pass(dominatorTree: DominatorTree) {
        val insertedPhis = insertPhis()
        val bbToMapValues = ReachingDefinitionAnalysis.run(cfg, dominatorTree)

        if (insertedPhis == 0) {
            // No phis were inserted, so no need to make steps further
            return
        }
        for (bb in cfg) {
            completePhis(bbToMapValues, bb)
        }

        val deadPool = hashSetOf<Instruction>()
        for (bb in cfg.postorder()) {
            removeRedundantPhis(deadPool, bb)
        }
    }
}