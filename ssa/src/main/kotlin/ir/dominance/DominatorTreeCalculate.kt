package ir.dominance

import ir.module.BasicBlocks
import ir.module.block.Block


internal object DominatorTreeCalculate : DominatorCalculate {
    override fun calculateIncoming(postorder: List<Block>, blockToIndex: Map<Block, Int>): Map<Int, List<Int>> {
        val predecessors = hashMapOf<Int, List<Int>>()

        for (bb in postorder) {
            val blockPredecessors = bb.predecessors()
            if (blockPredecessors.isEmpty()) {
                continue
            }

            val idx = blockToIndex[bb]
            assert(idx != null) { "Block not found in index: bb=${bb}" }
            predecessors[idx!!] = blockPredecessors.map {
                val i = blockToIndex[it]
                assert(i != null) { "Block not found in index: predecessor=$it, bb=${bb}" }
                i!!
            }
        }

        return predecessors
    }

    override fun blockOrdering(basicBlocks: BasicBlocks): List<Block> {
        return basicBlocks.postorder().order()
    }

    fun evaluate(basicBlocks: BasicBlocks): DominatorTree {
        return DominatorTree(calculate(basicBlocks))
    }
}