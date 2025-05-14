package ir.pass.transform

import common.arrayFrom
import common.forEachWith
import ir.pass.analysis.dominance.DominatorTree
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.pass.CompileContext
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.pass.analysis.JoinPointSetPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.analysis.traverse.PostOrderFabric
import ir.pass.transform.auxiliary.PhiFunctionPruning
import ir.pass.transform.auxiliary.RemoveDeadMemoryInstructions
import ir.pass.transform.auxiliary.RewritePrimitives
import ir.pass.transform.auxiliary.RewritePrimitivesUtil
import ir.types.Type
import ir.types.asType
import ir.value.Value
import ir.value.constant.UndefValue


class Mem2Reg internal constructor(module: Module, ctx: CompileContext): TransformPass(module, ctx) {
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
    override fun create(module: Module, ctx: CompileContext): TransformPass {
        return Mem2Reg(module.copy(), ctx)
    }
}

private class Mem2RegImpl(private val cfg: FunctionData) {
    private val joinSet = cfg.analysis(JoinPointSetPassFabric)

    private fun insertPhis(): List<UncompletedPhi> {
        val insertedPhis = arrayListOf<UncompletedPhi>()
        for ((bb, vSet) in joinSet) {
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
            uncompletedPhi.updateUsages(phi)
            uncompletedPhi.die(UndefValue)

            for ((i, v) in phi.operands().withIndex()) {
                val newValue = if (v == uncompletedPhi) phi else v
                phi.value(i, newValue)
            }
        }
    }

    fun pass(dominatorTree: DominatorTree) {
        val uncompletedPhis = insertPhis()
        val bbToMapValues = RewritePrimitivesUtil.run(cfg, dominatorTree)

        if (uncompletedPhis.isEmpty()) {
            // No phis were inserted, so no need to make steps further
            return
        }

        completePhis(bbToMapValues, uncompletedPhis)
    }
}