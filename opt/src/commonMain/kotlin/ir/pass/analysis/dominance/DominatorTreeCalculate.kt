package ir.pass.analysis.dominance

import common.assertion
import ir.module.FunctionData
import ir.module.Sensitivity
import ir.module.block.AnyBlock
import ir.pass.analysis.traverse.BlockOrder
import ir.pass.analysis.traverse.PostOrderFabric
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPassFabric


class DominatorTreeCalculate internal constructor(private val basicBlocks: FunctionData) : DominatorCalculate<DominatorTree>() {
    private val postorder = basicBlocks.analysis(PostOrderFabric)

    override fun calculateIncoming(postorder: BlockOrder, blockToIndex: Map<AnyBlock, Int>): Map<Int, List<Int>> {
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

    override fun blockOrdering(basicBlocks: FunctionData): BlockOrder {
        return postorder
    }

    override fun name(): String {
        return "DominatorTree"
    }

    override fun run(): DominatorTree {
        return DominatorTree(calculate(basicBlocks), basicBlocks.marker())
    }
}

object DominatorTreeFabric: FunctionAnalysisPassFabric<DominatorTree>() {
    override fun type(): AnalysisType {
        return AnalysisType.DOMINATOR_TREE
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): DominatorTree {
        return DominatorTreeCalculate(functionData).run()
    }
}