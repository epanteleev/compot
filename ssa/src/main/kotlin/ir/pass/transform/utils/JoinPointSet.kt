package ir.pass.transform.utils

import ir.DominatorTree
import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.BasicBlocks
import ir.module.block.AnyBlock


class JoinPointSet internal constructor(private val joinSet: Map<AnyBlock, MutableSet<Alloc>>) {
    operator fun iterator(): Iterator<Map.Entry<AnyBlock, Set<Alloc>>> {
        return joinSet.iterator()
    }

    companion object {
        fun evaluate(blocks: BasicBlocks, dominatorTree: DominatorTree): JoinPointSet {
            val df = dominatorTree.frontiers()
            return JoinPointSetEvaluate(blocks, df).calculate()
        }

        fun evaluate(blocks: BasicBlocks): JoinPointSet {
            val df = blocks.dominatorTree().frontiers()
            return JoinPointSetEvaluate(blocks, df).calculate()
        }
    }
}

private class JoinPointSetEvaluate(private val blocks: BasicBlocks, private val frontiers: Map<AnyBlock, List<AnyBlock>>) {
    private val joinSet = hashMapOf<AnyBlock, MutableSet<Alloc>>()

    private fun hasDef(bb: AnyBlock, variable: Alloc): Boolean {
        if (bb.instructions().contains(variable)) {
            return true
        }
        for (users in variable.usedIn()) {
            if (users !is Store) {
                continue
            }
            if (bb.instructions().contains(users)) {
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

            for (y in frontiers[x]!!) {
                if (phiPlaces.contains(y)) {
                    continue
                }
                val values = joinSet.getOrPut(y) { mutableSetOf() }

                values.add(v)
                phiPlaces.add(y)

                if (!hasDef(y, v)) {
                    stores.add(y)
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