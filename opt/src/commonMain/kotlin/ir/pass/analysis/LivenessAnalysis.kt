package ir.pass.analysis

import ir.value.LocalValue
import ir.instruction.Phi
import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.module.block.Label
import ir.pass.analysis.traverse.LinearScanOrderFabric
import ir.pass.common.AnalysisResult
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric


data class LiveInfo(internal var liveIn: MutableSet<LocalValue>, internal var liveOut: MutableSet<LocalValue>) {
    fun liveIn(): Set<LocalValue> = liveIn
    fun liveOut(): Set<LocalValue> = liveOut
}

class LivenessAnalysisInfo internal constructor(private val liveness: Map<Label, LiveInfo>, marker: MutationMarker): AnalysisResult(marker) {
    override fun toString(): String = buildString {
        for ((label, liveInfo) in liveness) {
            append("Label: $label\n")
            append("LiveIn: ${liveInfo.liveIn}\n")
            append("LiveOut: ${liveInfo.liveOut}\n")
        }
    }

    fun liveOut(label: Label): Set<LocalValue> {
        return liveness[label]!!.liveOut()
    }

    fun liveIn(label: Label): Set<LocalValue> {
        return liveness[label]!!.liveIn()
    }

    val size: Int
        get() = liveness.size
}

private data class KillGenSet(val kill: Set<LocalValue>, val gen: Set<LocalValue>)

// TODO Inefficient implementation, should be optimized
private class LivenessAnalysis(private val functionData: FunctionData): FunctionAnalysisPass<LivenessAnalysisInfo>() {
    private val linearScanOrder = functionData.analysis(LinearScanOrderFabric)
    private val liveness = run {
        val mapOf = hashMapOf<Label, LiveInfo>()
        for (bb in functionData) {
            mapOf[bb] = LiveInfo(hashSetOf(), hashSetOf())
        }

        mapOf
    }

    private fun computeLocalLiveSets(): Map<Label, KillGenSet> {
        val killGenSet = mutableMapOf<Label, KillGenSet>()
        for (bb in linearScanOrder) {
            val gen = hashSetOf<LocalValue>()
            val kill = hashSetOf<LocalValue>()

            for (inst in bb) {
                // Handle input operands
                if (inst !is Phi) {
                    for (operand in inst.operands()) {
                        if (operand !is LocalValue) {
                            continue
                        }

                        if (kill.contains(operand)) {
                            continue
                        }

                        gen.add(operand)
                    }
                }


                // Handle output operand
                if (inst is LocalValue) {
                    kill.add(inst)
                }
            }

            killGenSet[bb] = KillGenSet(kill, gen)
        }
        return killGenSet
    }

    private fun computeGlobalLiveSets() {
        val killGenSet = computeLocalLiveSets()
        do {
            var changed = false
            for (bb in linearScanOrder.reversed()) {
                val liveOut = mutableSetOf<LocalValue>()
                for (succ in bb.successors()) {
                    // live_out = b.live_out ∪ succ.live_in
                    liveOut.addAll(liveness[succ]!!.liveIn)
                }

                if (!liveness[bb]!!.liveOut.containsAll(liveOut)) {
                    changed = true
                    liveness[bb]!!.liveOut = liveOut
                }

                // live_in = (b.live_out – b.live_kill) ∪ b.live_gen
                val liveIn = run {
                    val clone = liveOut.toMutableSet()
                    clone.removeAll(killGenSet[bb]!!.kill)
                    clone.addAll(killGenSet[bb]!!.gen)
                    clone
                }
                liveness[bb]!!.liveIn = liveIn
            }
        } while (changed)

        for (bb in linearScanOrder) {
            bb.phis { phi ->
                val livenessBB = liveness[bb]!!
                phi.zip { block, value ->
                    if (value is LocalValue) {
                        liveness[block]!!.liveOut.add(value)
                        livenessBB.liveIn.add(value)
                    }
                }
            }
        }
    }

    override fun run(): LivenessAnalysisInfo {
        computeGlobalLiveSets()
        return LivenessAnalysisInfo(liveness, functionData.marker())
    }
}

object LivenessAnalysisPassFabric: FunctionAnalysisPassFabric<LivenessAnalysisInfo>() {
    override fun type(): AnalysisType {
        return AnalysisType.LIVENESS
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): LivenessAnalysisInfo {
        return LivenessAnalysis(functionData).run()
    }
}