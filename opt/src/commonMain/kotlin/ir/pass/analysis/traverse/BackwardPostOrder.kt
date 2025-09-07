package ir.pass.analysis.traverse

import ir.module.AnyFunctionData
import ir.module.FunctionData
import ir.module.Sensitivity
import ir.module.block.Block
import ir.module.block.Label
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


private class BackwardPostOrderPass<FD: AnyFunctionData>(private val functionData: FD): FunctionAnalysisPass<BlockOrder>() {
    override fun run(): BlockOrder {
        val visited = BooleanArray(functionData.size())
        val order = arrayListOf<Block>()
        val stack = arrayListOf<Block>()
        stack.add(functionData.end())

        while (stack.isNotEmpty()) {
            val bb = stack.removeLast()

            if (bb.equals(Label.entry)) {
                continue
            }

            if (visited[bb.index]) {
                continue
            }

            order.add(bb)
            visited[bb.index] = true

            val predecessors = bb.predecessors()
            for (idx in predecessors.indices.reversed()) {
                stack.add(predecessors[idx])
            }
        }

        order.add(functionData.begin())
        order.reverse()

        return BlockOrder(order, functionData.marker())
    }
}

object BackwardPostOrderFabric : FunctionAnalysisPassFabric<BlockOrder>() {
    override fun type(): AnalysisType {
        return AnalysisType.BACKWARD_POST_ORDER
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): BlockOrder {
        return BackwardPostOrderPass(functionData).run()
    }
}