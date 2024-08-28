package ir.pass.analysis.traverse

import ir.module.Sensitivity
import ir.module.FunctionData
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.traverse.iterator.PreorderIterator


private class PreOrderPass(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    override fun run(): BlockOrder {
        val order = PreorderIterator(functionData.begin(), functionData.size()).order()
        return BlockOrder(order, functionData.marker())
    }
}

object PreOrderFabric : FunctionAnalysisPassFabric<BlockOrder>() {
    override fun type(): AnalysisType {
        return AnalysisType.PRE_ORDER
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): BlockOrder {
        return PreOrderPass(functionData).run()
    }
}