package ir.pass.analysis

import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.iterator.LinearScanOrderIterator
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric


class LinearScanOrder(private val order: List<Block>): AnalysisResult(), Collection<Block> {
    override fun iterator(): Iterator<Block> {
        return order.iterator()
    }

    override val size: Int
        get() = order.size

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

class LinearScanOrderPass(private val functionData: FunctionData): FunctionAnalysisPass<LinearScanOrder>() {
    override fun name(): String {
        return "LinearScanOrder"
    }

    override fun run(): LinearScanOrder {
        val loopInfo = functionData.analysis(LoopDetectionPassFabric)
        val order = LinearScanOrderIterator(functionData.begin(), functionData.size(), loopInfo).order()
        return LinearScanOrder(order.toList())
    }
}

object LinearScanOrderFabric : FunctionAnalysisPassFabric<LinearScanOrder>() {
    override fun create(functionData: FunctionData): LinearScanOrderPass {
        return LinearScanOrderPass(functionData)
    }
}