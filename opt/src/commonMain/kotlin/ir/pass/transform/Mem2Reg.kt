package ir.pass.transform

import common.arrayFrom
import common.forEachWith
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
import ir.pass.transform.auxiliary.RemoveDeadMemoryInstructions
import ir.pass.transform.auxiliary.RewritePrimitives
import ir.pass.transform.auxiliary.RewritePrimitivesUtil
import ir.types.Type
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

    private fun insertPhis(): List<UncompletedPhi> {
        val insertedPhis = arrayListOf<UncompletedPhi>()
        for ((bb, vSet) in joinSet) { bb as Block
            val blocks = bb.predecessors().toTypedArray()
            for (v in vSet) {
                val phi = bb.prepend(UncompletedPhi.phi(v.allocatedType.asType(), v, blocks))
                insertedPhis.add(phi)
            }
        }

        return insertedPhis
    }

    private fun completePhis(bbToMapValues: RewritePrimitives, uncompletedPhis: List<UncompletedPhi>) {
        fun renameValues(block: Block, type: Type, v: Value): Value {
            return bbToMapValues.tryRename(block, type, v)?: UndefValue
        }

        val completedPhis = arrayListOf<Phi>()
        for (phi in uncompletedPhis) {
            val values = arrayFrom(phi.incoming()) { l ->
                renameValues(l, phi.type(), phi.value())
            }

            val newPhi = phi.owner().putBefore(phi, Phi.phi(phi.incoming().toTypedArray(), phi.type(), values))
            completedPhis.add(newPhi)
        }

        completedPhis.forEachWith(uncompletedPhis) { phi, uncompletedPhi ->
            uncompletedPhi.owner().updateUsages(uncompletedPhi) { phi }
            uncompletedPhi.owner().kill(uncompletedPhi, UndefValue)
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
        val uncompletedPhis = insertPhis()
        val bbToMapValues = RewritePrimitivesUtil.run(cfg, dominatorTree)

        if (uncompletedPhis.isEmpty()) {
            // No phis were inserted, so no need to make steps further
            return
        }

        completePhis(bbToMapValues, uncompletedPhis)

        for (bb in cfg.analysis(PostOrderFabric)) {
            removeRedundantPhis(bb)
        }
    }
}