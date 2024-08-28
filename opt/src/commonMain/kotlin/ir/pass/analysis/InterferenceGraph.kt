package ir.pass.analysis

import ir.module.FunctionData
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.value.LocalValue


class InterferenceGraph(private val graph: Map<LocalValue, Set<LocalValue>>): AnalysisResult() {
}


class InterferenceGraphBuilder(private val functionData: FunctionData): FunctionAnalysisPass<InterferenceGraph>() {
    private val liveIntervals = functionData.analysis(LiveIntervalsFabric)

    private fun build(): InterferenceGraph {
        val graph = mutableMapOf<LocalValue, MutableSet<LocalValue>>()
        TODO()
    }

    override fun name(): String {
        return "InterferenceGraphBuilder"
    }

    override fun run(): InterferenceGraph {
        //return build()
        TODO()
    }
}

object InterferenceGraphFabric : FunctionAnalysisPassFabric<InterferenceGraph>() {
    override fun create(functionData: FunctionData): InterferenceGraphBuilder {
        return InterferenceGraphBuilder(functionData)
    }
}