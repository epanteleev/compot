package ir.pass.transform

import ir.pass.analysis.dominance.DominatorTree
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.pass.TransformPassFabric
import ir.pass.TransformPass
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.transform.utils.*
import ir.pass.transform.auxiliary.RemoveDeadMemoryInstructions
import ir.types.PrimitiveType
import ir.types.Type
import ir.value.Value


data class Mem2RegException(override val message: String): Exception(message)

class Mem2Reg internal constructor(module: Module): TransformPass(module) {
    override fun name(): String = "mem2reg"
    override fun run(): Module {
        module.functions.forEach { fnData ->
            val dominatorTree = fnData.analysis(DominatorTreeFabric)
            val joinSet = JoinPointSet.evaluate(fnData, dominatorTree)
            Mem2RegImpl(fnData, joinSet).pass(dominatorTree)
        }
        return PhiFunctionPruning.run(RemoveDeadMemoryInstructions.run(module))
    }
}

object Mem2RegFabric: TransformPassFabric() {
    override fun create(module: Module): TransformPass {
        return Mem2Reg(module.copy())
    }
}

private class Mem2RegImpl(private val cfg: FunctionData, private val joinSet: JoinPointSet) {
    private fun insertPhis(): Set<Phi> {
        val insertedPhis = hashSetOf<Phi>()
        for ((bb, vSet) in joinSet) { bb as Block
            for (v in vSet) {
                val phi = bb.prepend { it.uncompletedPhi(v.allocatedType as PrimitiveType, v) }
                insertedPhis.add(phi)
            }
        }

        return insertedPhis
    }

    private fun completePhis(bbToMapValues: ReachingDefinition, insertedPhis: Set<Phi>) {
        fun renameValues(block: Block, v: Value, expectedType: Type): Value {
            return bbToMapValues.tryRename(block, v, expectedType)?: Value.UNDEF
        }

        for (phi in insertedPhis) {
            val newUsages = arrayListOf<Value>()
            phi.updateDataFlow { l, v -> renameValues(l, v, phi.type()) }
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

        if (insertedPhis.isEmpty()) {
            // No phis were inserted, so no need to make steps further
            return
        }

        completePhis(bbToMapValues, insertedPhis)

        val deadPool = hashSetOf<Instruction>()
        for (bb in cfg.postorder()) {
            removeRedundantPhis(deadPool, bb)
        }
    }
}