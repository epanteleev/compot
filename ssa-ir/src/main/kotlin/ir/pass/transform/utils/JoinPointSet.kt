package ir.pass.transform.utils

import ir.*
import ir.block.AnyBlock
import ir.instruction.ValueInstruction

class JoinPointSet private constructor(private val blocks: BasicBlocks, private val frontiers: Map<AnyBlock, List<AnyBlock>>) {
    private val joinSet = hashMapOf<AnyBlock, MutableSet<ValueInstruction>>()

    private fun hasStore(bb: AnyBlock, variable: ValueInstruction): Boolean {
        return bb.instructions().contains(variable)
    }

    operator fun iterator(): Iterator<Map.Entry<AnyBlock, Set<ValueInstruction>>> {
        return joinSet.iterator()
    }

    private fun calculateForVariable(v: ValueInstruction, stores: MutableSet<AnyBlock>) {
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

                if (!hasStore(y, v)) {
                    stores.add(y)
                }
            }
        }
    }

    private fun calculate(): JoinPointSet {
        val allocInfo = AllocStoreInfo.create(blocks)
        val stores = allocInfo.allStores()

        for ((v, vStores) in stores) {
            calculateForVariable(v, vStores as MutableSet<AnyBlock>)
        }

        return this
    }

    companion object {
        fun evaluate(blocks: BasicBlocks, dominatorTree: DominatorTree): JoinPointSet {
            val df = dominatorTree.frontiers()
            return JoinPointSet(blocks, df).calculate()
        }

        fun evaluate(blocks: BasicBlocks): JoinPointSet {
            val df = blocks.dominatorTree().frontiers()
            return JoinPointSet(blocks, df).calculate()
        }
    }
}