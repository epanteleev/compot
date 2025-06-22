package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.module.block.Block
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


private class BfsOrderPass(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    private val stack = arrayListOf<List<Block>>()
    private val visited = BooleanArray(functionData.size())
    private val order = arrayListOf<Block>()

    private fun visitBlock(bb: Block) {
        order.add(bb)
        visited[bb.index] = true
        if (bb.successors().isNotEmpty()) {
            stack.add(bb.successors())
        }
    }

    override fun run(): BlockOrder {
        visitBlock(functionData.begin())

        while (stack.isNotEmpty()) {
            val basicBlocks = stack.removeLast()

            for (bb in basicBlocks) {
                if (visited[bb.index]) {
                    continue
                }
                visitBlock(bb)
            }
        }

        return BlockOrder(order, functionData.marker())
    }
}

object BfsOrderOrderFabric : FunctionAnalysisPassFabric<BlockOrder>() {
    override fun type(): AnalysisType {
        return AnalysisType.BFS_ORDER
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): BlockOrder {
        return BfsOrderPass(functionData).run()
    }
}