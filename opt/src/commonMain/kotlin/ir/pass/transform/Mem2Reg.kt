package ir.pass.transform

import ir.pass.analysis.dominance.DominatorTree
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.pass.analysis.JoinPointSetPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.analysis.traverse.PostOrderFabric
import ir.pass.transform.utils.*
import ir.pass.transform.auxiliary.RemoveDeadMemoryInstructions
import ir.types.asType
import ir.value.Value
import ir.value.constant.UndefValue


class Mem2Reg internal constructor(module: Module): TransformPass(module) {
    override fun name(): String = "mem2reg"
    override fun run(): Module {
        module.functions.values.forEach { fnData ->
            val dominatorTree = fnData.analysis(DominatorTreeFabric)
            Mem2RegImpl(fnData).pass(dominatorTree)
        }
        return PhiFunctionPruning.run(RemoveDeadMemoryInstructions.run(module))
    }
}

object Mem2RegFabric: TransformPassFabric() {
    override fun create(module: Module): TransformPass {
        return Mem2Reg(module.copy())
    }
}

private class Mem2RegImpl(private val cfg: FunctionData) {
    private val joinSet = cfg.analysis(JoinPointSetPassFabric)

    private fun insertPhis(): Set<Phi> {
        val insertedPhis = hashSetOf<Phi>()
        for ((bb, vSet) in joinSet) { bb as Block
            val blocks = bb.predecessors().mapTo(arrayListOf()) { it }
            for (v in vSet) {
                val phi = bb.prepend(Phi.phiUncompleted(v.allocatedType.asType(), v, blocks))
                insertedPhis.add(phi)
            }
        }

        return insertedPhis
    }

    private fun completePhis(bbToMapValues: RewritePrimitives, insertedPhis: Set<Phi>) {
        fun renameValues(block: Block, v: Value): Value {
            return bbToMapValues.tryRename(block, v)?: UndefValue
        }

        for (phi in insertedPhis) {
            phi.owner().updateDF(phi) { l, v -> renameValues(l, v) }
        }
    }

    // Remove unused phis
    private fun removeRedundantPhis(bb: Block) {
        val deadPool = hashSetOf<Instruction>()
        fun filter(bb: Block, instruction: Instruction): Instruction? {
            if (instruction !is Phi) {
                return instruction
            }

            if (instruction.usedIn().isEmpty() || deadPool.containsAll(instruction.usedIn())) {
                deadPool.add(instruction)
                return bb.kill(instruction, UndefValue)
            }

            return instruction
        }

        bb.transform { filter(bb, it) }
    }

    fun pass(dominatorTree: DominatorTree) {
        val insertedPhis = insertPhis()
        val bbToMapValues = RewritePrimitivesUtil.run(cfg, insertedPhis, dominatorTree)

        if (insertedPhis.isEmpty()) {
            // No phis were inserted, so no need to make steps further
            return
        }

        completePhis(bbToMapValues, insertedPhis)

        for (bb in cfg.analysis(PostOrderFabric)) {
            removeRedundantPhis(bb)
        }
    }
}