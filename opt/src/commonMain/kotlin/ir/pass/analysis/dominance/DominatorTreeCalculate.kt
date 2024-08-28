package ir.pass.analysis.dominance

import common.assertion
import ir.module.FunctionData
import ir.module.block.AnyBlock
import ir.pass.FunctionAnalysisPassFabric


class DominatorTreeCalculate internal constructor(private val basicBlocks: FunctionData) : DominatorCalculate<DominatorTree>() {
    override fun calculateIncoming(postorder: List<AnyBlock>, blockToIndex: Map<AnyBlock, Int>): Map<Int, List<Int>> {
        val predecessors = hashMapOf<Int, List<Int>>()

        for (bb in postorder) {
            val blockPredecessors = bb.predecessors()
            if (blockPredecessors.isEmpty()) {
                continue
            }

            val idx = blockToIndex[bb]
            assertion(idx != null) { "Block not found in index: bb=${bb}" }
            predecessors[idx!!] = blockPredecessors.map {
                val i = blockToIndex[it]
                assertion(i != null) { "Block not found in index: predecessor=$it, bb=${bb}" }
                i!!
            }
        }

        return predecessors
    }

    override fun blockOrdering(basicBlocks: FunctionData): List<AnyBlock> {
        return basicBlocks.postorder().order()
    }

    override fun name(): String {
        return "DominatorTree"
    }

    override fun run(): DominatorTree {
        return DominatorTree(calculate(basicBlocks))
    }
}

object DominatorTreeFabric: FunctionAnalysisPassFabric<DominatorTree>() {
    override fun create(functionData: FunctionData): DominatorTreeCalculate {
        return DominatorTreeCalculate(functionData)
    }
}