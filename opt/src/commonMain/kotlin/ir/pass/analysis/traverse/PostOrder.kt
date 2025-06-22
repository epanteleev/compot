package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


private class PostOrderPass(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    override fun run(): BlockOrder {
        val preorder = functionData.analysis(PreOrderFabric)
        return BlockOrder(preorder.reversed(), functionData.marker())
    }
}

object PostOrderFabric : FunctionAnalysisPassFabric<BlockOrder>() {
    override fun type(): AnalysisType {
        return AnalysisType.POST_ORDER
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): BlockOrder {
        return PostOrderPass(functionData).run()
    }
}