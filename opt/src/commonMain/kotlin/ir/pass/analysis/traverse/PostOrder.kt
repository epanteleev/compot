package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.pass.analysis.traverse.iterator.PostorderIterator
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric

class PostOrderPass internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    override fun name(): String {
        return "PostOrder"
    }

    override fun run(): BlockOrder {
        val order = PostorderIterator(functionData.begin(), functionData.size()).order()
        return BlockOrder(order, functionData.marker())
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