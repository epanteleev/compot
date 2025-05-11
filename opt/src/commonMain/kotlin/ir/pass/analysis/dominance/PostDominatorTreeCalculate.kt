package ir.pass.analysis.dominance

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.module.block.Block
import ir.pass.analysis.traverse.BackwardPostOrderFabric
import ir.pass.analysis.traverse.BlockOrder
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPassFabric


private class PostDominatorTreeCalculate(private val basicBlocks: FunctionData) : DominatorCalculate<PostDominatorTree>() {
    private val backwardPostorder = basicBlocks.analysis(BackwardPostOrderFabric)

    override fun calculateIncoming(postorder: BlockOrder, blockToIndex: Map<Block, Int>): Map<Int, List<Int>> {
        val successors = hashMapOf<Int, List<Int>>()

        for (bb in postorder) {
            val blockSuccessors = bb.successors()
            if (blockSuccessors.isEmpty()) {
                continue
            }

            successors[blockToIndex[bb]!!] = blockSuccessors.map { blockToIndex[it]!! }
        }

        return successors
    }

    override fun blockOrdering(basicBlocks: FunctionData): BlockOrder {
        return backwardPostorder
    }

    override fun run(): PostDominatorTree {
        val domTree = calculate(basicBlocks)
        return PostDominatorTree(domTree[basicBlocks.begin()]!!, domTree, basicBlocks.marker())
    }
}

object PostDominatorTreeFabric: FunctionAnalysisPassFabric<PostDominatorTree>() {
    override fun type(): AnalysisType {
        return AnalysisType.POST_DOMINATOR_TREE
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): PostDominatorTree {
        return PostDominatorTreeCalculate(functionData).run()
    }
}