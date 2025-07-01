package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.module.block.Block
import ir.pass.analysis.LoopDetectionPassFabric
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


private class LinearScanOrderPass(private val functionData: FunctionData) : FunctionAnalysisPass<BlockOrder>() {
    private val loopInfo = functionData.analysis(LoopDetectionPassFabric)

    override fun run(): BlockOrder {
        val visited = BooleanArray(functionData.size())
        val order = arrayListOf<Block>()
        val stack = arrayListOf<Block>()
        stack.add(functionData.begin())

        val exitBlock: Block = functionData.end()
        while (stack.isNotEmpty()) {
            val bb = stack.removeLast()
            if (bb == exitBlock) {
                continue
            }

            if (visited[bb.index]) {
                continue
            }
            order.add(bb)
            visited[bb.index] = true

            val successors = bb.successors()
            for (idx in successors.indices.reversed()) {
                stack.add(successors[idx])
            }

            // Special case for loops
            val loopInfo = loopInfo[bb]
            if (loopInfo != null) {
                // Add loop exit and loop enter blocks to the stack
                // Order of adding isn't important
                stack.add(loopInfo.first().exit())
                stack.add(loopInfo.first().enter())
            }
        }

        order.add(exitBlock)

        return BlockOrder(order, functionData.marker())
    }
}

object LinearScanOrderFabric : FunctionAnalysisPassFabric<BlockOrder>() {
    override fun type(): AnalysisType {
        return AnalysisType.LINEAR_SCAN_ORDER
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): BlockOrder {
        return LinearScanOrderPass(functionData).run()
    }
}