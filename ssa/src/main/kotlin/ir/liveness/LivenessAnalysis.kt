package ir.liveness

import ir.LocalValue
import common.intMapOf
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label


data class LiveInfo(internal var liveIn: MutableSet<LocalValue>, internal val liveOut: MutableSet<LocalValue>) {
    fun liveIn(): Set<LocalValue> = liveIn
    fun liveOut(): Set<LocalValue> = liveOut
}

private data class KillGenSet(val kill: Set<LocalValue>, val gen: Set<LocalValue>)


class LivenessAnalysis private constructor(val data: FunctionData, private val linearScanOrder: List<Block>) {
    private val liveness = run {
        val mapOf = intMapOf<Label, LiveInfo>(data.blocks.size()) { it.index }
        for (bb in data.blocks) {
            mapOf[bb] = LiveInfo(mutableSetOf(), mutableSetOf())
        }

        mapOf
    }

    private fun computeLocalLiveSets(): Map<Block, KillGenSet> {
        val killGenSet = intMapOf<Block, KillGenSet>(data.blocks.size()) { it.index }
        for (bb in linearScanOrder) {
            val gen = mutableSetOf<LocalValue>()
            val kill = mutableSetOf<LocalValue>()

            for (inst in bb.instructions()) {
                // Handle input operands
                for (usage in inst.operands()) {
                    if (usage !is LocalValue) {
                        continue
                    }

                    if (kill.contains(usage)) {
                        continue
                    }

                    gen.add(usage)
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
                val liveOut = liveness[bb]!!.liveOut
                for (succ in bb.successors()) {
                    changed = changed or liveOut.addAll(liveness[succ]!!.liveIn)
                }

                val liveIn = (liveOut.toMutableSet() - killGenSet[bb]!!.kill) + killGenSet[bb]!!.gen
                changed = changed or (liveIn != liveness[bb]!!.liveIn)
                liveness[bb]!!.liveIn = liveIn.toMutableSet()
            }
        } while (changed)
    }

    companion object {
        fun evaluate(data: FunctionData, blockOrder: List<Block>): Map<Label, LiveInfo> {
            val analysis = LivenessAnalysis(data, blockOrder)
            analysis.computeGlobalLiveSets()
            return analysis.liveness
        }

        fun evaluate(data: FunctionData): Map<Label, LiveInfo> {
            val loopInfo = data.blocks.loopInfo()
            return evaluate(data, data.blocks.linearScanOrder(loopInfo).order())
        }
    }
}