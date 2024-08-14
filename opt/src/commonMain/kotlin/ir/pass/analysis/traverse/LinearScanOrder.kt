package ir.pass.analysis.traverse

import ir.module.FunctionData
import ir.module.Sensitivity
import ir.pass.analysis.LoopDetectionPassFabric
import ir.pass.analysis.traverse.iterator.LinearScanOrderIterator
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


class LinearScanOrderPass internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<BlockOrder>() {
    private val loopInfo = functionData.analysis(LoopDetectionPassFabric)

    override fun name(): String {
        return "LinearScanOrder"
    }

    override fun run(): BlockOrder {
        val order = LinearScanOrderIterator(functionData.begin(), functionData.size(), loopInfo).order()
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