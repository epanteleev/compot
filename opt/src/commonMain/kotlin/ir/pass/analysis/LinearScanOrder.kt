package ir.pass.analysis

import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.module.block.Block
import ir.module.block.iterator.LinearScanOrderIterator
import ir.pass.common.AnalysisResult
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


class LinearScanOrder(private val order: List<Block>, marker: MutationMarker): AnalysisResult(marker), Collection<Block> {
    override fun iterator(): Iterator<Block> {
        return order.iterator()
    }

    override val size: Int
        get() = order.size

    operator fun get(index: Int): Block {
        return order[index]
    }

    override fun isEmpty(): Boolean {
        return order.isEmpty()
    }

    override fun containsAll(elements: Collection<Block>): Boolean {
        return order.containsAll(elements)
    }

    override fun contains(element: Block): Boolean {
        return order.contains(element)
    }
}

class LinearScanOrderPass internal constructor(private val functionData: FunctionData): FunctionAnalysisPass<LinearScanOrder>() {
    override fun name(): String {
        return "LinearScanOrder"
    }

    override fun run(): LinearScanOrder {
        val loopInfo = functionData.analysis(LoopDetectionPassFabric)
        val order = LinearScanOrderIterator(functionData.begin(), functionData.size(), loopInfo).order()
        return LinearScanOrder(order.toList(), functionData.marker())
    }
}

object LinearScanOrderFabric : FunctionAnalysisPassFabric<LinearScanOrder>() {
    override fun type(): AnalysisType {
        return AnalysisType.LINEAR_SCAN_ORDER
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_FLOW
    }

    override fun create(functionData: FunctionData): LinearScanOrder {
        return LinearScanOrderPass(functionData).run()
    }
}