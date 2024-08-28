package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.pass.analysis.traverse.iterator.BfsTraversalIterator
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


class BfsOrderPass internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    override fun run(): BlockOrder {
        val order = BfsTraversalIterator(functionData.begin(), functionData.size()).order()
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