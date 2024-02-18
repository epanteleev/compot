package ir.dominance

import ir.module.BasicBlocks
import ir.module.block.AnyBlock


internal object DominatorTreeCalculate : DominatorCalculate {
    override fun calculateIncoming(postorder: List<AnyBlock>, blockToIndex: Map<AnyBlock, Int>): Map<Int, List<Int>> {
        val predecessors = hashMapOf<Int, List<Int>>()

        for (bb in postorder) {
            val blockPredecessors = bb.predecessors()
            if (blockPredecessors.isEmpty()) {
                continue
            }

            predecessors[blockToIndex[bb]!!] = blockPredecessors.map { blockToIndex[it]!! }
        }

        return predecessors
    }

    override fun blockOrdering(basicBlocks: BasicBlocks): List<AnyBlock> {
        return basicBlocks.postorder().order()
    }

    fun evaluate(basicBlocks: BasicBlocks): DominatorTree {
        return DominatorTree(calculate(basicBlocks))
    }
}