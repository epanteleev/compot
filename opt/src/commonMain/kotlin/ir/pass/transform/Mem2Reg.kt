package ir.pass.transform

import ir.pass.analysis.dominance.DominatorTree
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.pass.CompileContext
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.pass.analysis.JoinPointSetPassFabric
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.transform.auxiliary.PhiFunctionPruning
import ir.pass.transform.auxiliary.RemoveDeadMemoryInstructions
import ir.pass.transform.auxiliary.RewritePrimitives
import ir.pass.transform.auxiliary.RewritePrimitivesUtil
import ir.types.Type
import ir.types.asType
import ir.value.Value
import ir.value.constant.UndefValue


class Mem2Reg internal constructor(module: SSAModule, ctx: CompileContext): TransformPass<SSAModule>(module, ctx) {
    override fun name(): String = "mem2reg"
    override fun run(): SSAModule {
        module.functions.values.forEach { fnData ->
            val dominatorTree = fnData.analysis(DominatorTreeFabric)
            Mem2RegImpl(fnData).pass(dominatorTree)
        }

        return PhiFunctionPruning.run(RemoveDeadMemoryInstructions.run(module))
    }
}

object Mem2RegFabric: TransformPassFabric<SSAModule>() {
    override fun create(module: SSAModule, ctx: CompileContext): TransformPass<SSAModule> {
        return Mem2Reg(module.copy(), ctx)
    }
}

private class Mem2RegImpl(private val cfg: FunctionData) {
    private val joinSet = cfg.analysis(JoinPointSetPassFabric)

    private fun insertPhis(): Map<Phi, Alloc> {
        val insertedPhis = hashMapOf<Phi, Alloc>()
        for ((bb, vSet) in joinSet) {
            val blocks = bb.predecessors().toTypedArray()
            for (v in vSet) {
                val phi = bb.prepend(Phi.undef(v.allocatedType.asType(), blocks))
                insertedPhis[phi] = v
            }
        }

        return insertedPhis
    }

    private fun completePhis(bbToMapValues: RewritePrimitives, uncompletedPhis: Map<Phi, Alloc>) {
        fun renameValues(block: Block, type: Type, v: Value): Value {
            return bbToMapValues.tryRename(block, type, v)?: UndefValue
        }

        for ((phi, alloc) in uncompletedPhis) {
            phi.values { block, value -> renameValues(block, phi.type(), alloc) }
        }
    }

    fun pass(dominatorTree: DominatorTree) {
        val uncompletedPhis = insertPhis()
        val bbToMapValues = RewritePrimitivesUtil.run(cfg, uncompletedPhis, dominatorTree)

        if (uncompletedPhis.isEmpty()) {
            // No phis were inserted, so no need to make steps further
            return
        }

        completePhis(bbToMapValues, uncompletedPhis)
    }
}