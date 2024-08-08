package ir.pass.analysis

import ir.module.FunctionData
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.value.LocalValue


class InterferenceGraph(private val graph: MutableMap<LocalValue, MutableSet<LocalValue>>): AnalysisResult() {
    internal fun addEdge(from: LocalValue, to: LocalValue) {
        graph[from]!!.add(to)
        graph[to]!!.add(from)
    }
}

class InterferenceGraphBuilder(functionData: FunctionData): FunctionAnalysisPass<InterferenceGraph>() {
    private val liveIntervals = functionData.analysis(LiveIntervalsFabric)
    private val interferenceGraph = InterferenceGraph(mutableMapOf())

    override fun run(): InterferenceGraph {
        //TODO Absolutely inefficient!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
        // O(n^2) complexity
        for ((v1, interval1) in liveIntervals) {
            for ((v2, interval2) in liveIntervals) {
                if (interval1 == interval2) {
                    continue
                }

                if (interval1.intersect(interval2)) {
                    interferenceGraph.addEdge(v1,v2)
                }
            }
        }

        return interferenceGraph
    }

    override fun name(): String {
        return "InterferenceGraphBuilder"
    }
}

object InterferenceGraphFabric : FunctionAnalysisPassFabric<InterferenceGraph>() {
    override fun create(functionData: FunctionData): InterferenceGraphBuilder {
        return InterferenceGraphBuilder(functionData)
    }
}