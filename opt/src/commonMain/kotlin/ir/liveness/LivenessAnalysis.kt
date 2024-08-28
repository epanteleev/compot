package ir.liveness

import ir.value.LocalValue
import common.intMapOf
import ir.instruction.Phi
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label


data class LiveInfo(internal var liveIn: MutableSet<LocalValue>, internal var liveOut: MutableSet<LocalValue>) {
    fun liveIn(): Set<LocalValue> = liveIn
    fun liveOut(): Set<LocalValue> = liveOut
}

private data class KillGenSet(val kill: Set<LocalValue>, val gen: Set<LocalValue>)

// TODO Inefficient implementation, should be optimized
class LivenessAnalysis private constructor(val data: FunctionData, private val linearScanOrder: List<Block>) {
    private val liveness = run {
        val mapOf = mutableMapOf<Label, LiveInfo>()
        for (bb in data) {
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
                    for (usage in inst.operands()) {
                        if (usage !is LocalValue) {
                            continue
                        }

                        if (kill.contains(usage)) {
                            continue
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

    companion object {
        fun evaluate(data: FunctionData, blockOrder: List<Block>): Map<Label, LiveInfo> {
            val analysis = LivenessAnalysis(data, blockOrder)
            analysis.computeGlobalLiveSets()
            return analysis.liveness
        }

        fun evaluate(data: FunctionData): Map<Label, LiveInfo> {
            val loopInfo = data.loopInfo()
            return evaluate(data, data.linearScanOrder(loopInfo).order())
        }
    }
}