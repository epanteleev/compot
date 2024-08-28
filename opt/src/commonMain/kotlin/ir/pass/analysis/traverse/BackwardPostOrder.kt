package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.traverse.iterator.BackwardPostorderIterator


class BackwardPostOrderPass internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    override fun run(): BlockOrder {
        val order = BackwardPostorderIterator(functionData.end(), functionData.size()).order()
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