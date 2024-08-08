package ir.pass.analysis

import ir.value.LocalValue
import ir.instruction.Phi
import ir.module.FunctionData
import ir.module.block.Label
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPass
import ir.pass.FunctionAnalysisPassFabric


data class LiveInfo(internal var liveIn: MutableSet<LocalValue>, internal var liveOut: MutableSet<LocalValue>) {
    fun liveIn(): Set<LocalValue> = liveIn
    fun liveOut(): Set<LocalValue> = liveOut
}

class LivenessAnalysisInfo(private val liveness: Map<Label, LiveInfo>): AnalysisResult() {
    operator fun get(bb: Label): LiveInfo {
        return liveness[bb]!!
    }

    val size: Int
        get() = liveness.size
}

private data class KillGenSet(val kill: Set<LocalValue>, val gen: Set<LocalValue>)

// TODO Inefficient implementation, should be optimized
class LivenessAnalysis internal constructor(private val functionData: FunctionData, private val linearScanOrder: LinearScanOrder): FunctionAnalysisPass<LivenessAnalysisInfo>() {
    private val liveness = run {
        val mapOf = mutableMapOf<Label, LiveInfo>()
        for (bb in functionData) {
            mapOf[bb] = LiveInfo(mutableSetOf(), mutableSetOf())
        }

        mapOf
    }

    private fun computeLocalLiveSets(): Map<Label, KillGenSet> {
        val killGenSet = mutableMapOf<Label, KillGenSet>()
        for (bb in linearScanOrder) {
            val gen = mutableSetOf<LocalValue>()
            val kill = mutableSetOf<LocalValue>()

            for (inst in bb) {
                // Handle input operands
                if (inst !is Phi) {
                    inst.operands { usage ->
                        if (usage !is LocalValue) {
                            return@operands
                        }

                        if (kill.contains(usage)) {
                            return@operands
                        }

                        gen.add(usage)
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
                phi.zip { block, value ->
                    if (value is LocalValue) {
                        liveness[block]!!.liveOut.add(value)
                        liveness[bb]!!.liveIn.add(value)
                    }
                }
            }
        }
    }

    override fun name(): String {
        return "LivenessAnalysis"
    }

    override fun run(): LivenessAnalysisInfo {
        computeGlobalLiveSets()
        return LivenessAnalysisInfo(liveness)
    }
}

object LivenessAnalysisPassFabric: FunctionAnalysisPassFabric<LivenessAnalysisInfo>() {
    override fun create(functionData: FunctionData): LivenessAnalysis {
        val linearScanOrder = functionData.analysis(LinearScanOrderFabric)
        return LivenessAnalysis(functionData, linearScanOrder)
    }
}