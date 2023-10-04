package ir.utils

import ir.*

class JoinPointSet private constructor(private val blocks: BasicBlocks, private val frontiers: Map<BasicBlock, List<BasicBlock>>) {
    private val joinSet = hashMapOf<BasicBlock, MutableSet<ValueInstruction>>()

    private fun hasStore(bb: BasicBlock, variable: ValueInstruction): Boolean {
        return bb.instructions().contains(variable)
    }

    private fun calculateForVariable(v: ValueInstruction, stores: MutableSet<BasicBlock>) {
        val phiPlaces = mutableSetOf<BasicBlock>()

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

                if (!hasStore(y, v)) {
                    stores.add(y)
                }
            }
        }
    }

    private fun calculate(): Map<BasicBlock, Set<Value>> {
        val allocInfo = AllocStoreInfo.create(blocks)
        val stores = allocInfo.allStores()

        for ((v, vStores) in stores) {
            calculateForVariable(v, vStores as MutableSet<BasicBlock>)
        }

        return joinSet
    }

    companion object {
        fun evaluate(blocks: BasicBlocks, dominatorTree: DominatorTree): Map<BasicBlock, Set<Value>> {
            val df = dominatorTree.frontiers()
            return JoinPointSet(blocks, df).calculate()
        }

        fun evaluate(blocks: BasicBlocks): Map<BasicBlock, Set<Value>> {
            val df = blocks.dominatorTree().frontiers()
            return JoinPointSet(blocks, df).calculate()
        }
    }
}