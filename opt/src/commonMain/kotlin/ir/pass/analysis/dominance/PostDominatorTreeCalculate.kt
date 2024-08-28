package ir.pass.analysis.dominance

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.module.block.AnyBlock
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPassFabric


internal class PostDominatorTreeCalculate internal constructor(private val basicBlocks: FunctionData) : DominatorCalculate<PostDominatorTree>() {
    override fun calculateIncoming(postorder: List<AnyBlock>, blockToIndex: Map<AnyBlock, Int>): Map<Int, List<Int>> {
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

    override fun blockOrdering(basicBlocks: FunctionData): List<AnyBlock> {
        return basicBlocks.backwardPostorder().order()
    }

    override fun name(): String {
        return "PostDominatorTree"
    }

    override fun run(): PostDominatorTree {
        return PostDominatorTree(calculate(basicBlocks), basicBlocks.marker())
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