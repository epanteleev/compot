package ir.pass.analysis

import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.pass.common.AnalysisResult
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.pass.common.AnalysisType
import ir.value.LocalValue


class InterferenceGraph(private val graph: MutableMap<LocalValue, MutableSet<LocalValue>>, marker: MutationMarker): AnalysisResult(marker) {
    internal fun addEdge(from: LocalValue, to: LocalValue) {
        val fromEdge = graph[from]
        if (fromEdge == null) {
            graph[from] = mutableSetOf(to)
        } else {
            fromEdge.add(to)
        }

        val toEdge = graph[to]
        if (toEdge == null) {
            graph[to] = mutableSetOf(from)
        } else {
            toEdge.add(from)
        }
    }

    fun neighbors(value: LocalValue): Set<LocalValue> {
        return graph[value]!!
    }
}

class InterferenceGraphBuilder(functionData: FunctionData): FunctionAnalysisPass<InterferenceGraph>() {
    private val liveIntervals = functionData.analysis(LiveIntervalsFabric)
    private val interferenceGraph = InterferenceGraph(mutableMapOf(), functionData.marker())

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
    override fun type(): AnalysisType {
        return AnalysisType.INTERFERENCE_GRAPH
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): InterferenceGraph {
        return InterferenceGraphBuilder(functionData).run()
    }
}