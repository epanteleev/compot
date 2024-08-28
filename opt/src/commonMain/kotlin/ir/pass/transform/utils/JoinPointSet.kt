package ir.pass.transform.utils

import common.intMapOf
import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.BasicBlocks
import ir.module.block.Label
import ir.module.block.AnyBlock
import ir.pass.analysis.dominance.DominatorTree
import ir.module.FunctionData
import ir.pass.analysis.dominance.DominatorTreeFabric


class JoinPointSet internal constructor(private val joinSet: Map<AnyBlock, MutableSet<Alloc>>) {
    operator fun iterator(): Iterator<Map.Entry<AnyBlock, Set<Alloc>>> {
        return joinSet.iterator()
    }

    companion object {
        fun evaluate(blocks: FunctionData, dominatorTree: DominatorTree): JoinPointSet {
            val df = dominatorTree.frontiers()
            return JoinPointSetEvaluate(blocks, df).calculate()
        }
    }
}

private class JoinPointSetEvaluate(private val blocks: FunctionData, private val frontiers: Map<AnyBlock, List<AnyBlock>>) {
    private val joinSet = intMapOf<AnyBlock, MutableSet<Alloc>>(blocks.size()) { bb: Label -> bb.index }

    private fun hasUserInBlock(bb: AnyBlock, variable: Alloc): Boolean {
        if (bb === variable.owner()) {
            return true
        }
        for (users in variable.usedIn()) {
            if (users !is Store) {
                continue
            }
            if (bb === users.owner()) {
                return true
            }
        }
        return false
    }

    private fun calculateForVariable(v: Alloc, stores: MutableSet<AnyBlock>) {
        val phiPlaces = mutableSetOf<AnyBlock>()

        while (stores.isNotEmpty()) {
            val x = stores.first()
            stores.remove(x)

            if (!frontiers.contains(x)) {
                continue
            }

            for (frontier in frontiers[x]!!) {
                if (phiPlaces.contains(frontier)) {
                    continue
                }
                val values = joinSet.getOrPut(frontier) { mutableSetOf() }

                values.add(v)
                phiPlaces.add(frontier)

                if (!hasUserInBlock(frontier, v)) {
                    stores.add(frontier)
                }
            }
        }
    }

    fun calculate(): JoinPointSet {
        val allocInfo = AllocStoreInfo.create(blocks)
        val stores = allocInfo.allStores()

        for ((v, vStores) in stores) {
            calculateForVariable(v, vStores as MutableSet<AnyBlock>)
        }

        return JoinPointSet(joinSet)
    }
}