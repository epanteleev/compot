package ir.platform.x64.regalloc.liveness

import common.intMapOf
import ir.LocalValue
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label


data class LiveInfo(internal var liveIn: MutableSet<LocalValue>, internal val liveOut: MutableSet<LocalValue>) {
    fun liveIn(): Set<LocalValue> = liveIn
    fun liveOut(): Set<LocalValue> = liveOut
}

private data class KillGenSet(val kill: Set<LocalValue>, val gen: Set<LocalValue>)


class NewLivenessAnalysis private constructor(val data: FunctionData) {
    private val loopInfo = data.blocks.loopInfo()
    private val linearScanOrder = data.blocks.linearScanOrder(loopInfo).order()
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
        fun evaluate(data: FunctionData): Map<Label, LiveInfo> {
            val analysis = NewLivenessAnalysis(data)
            analysis.computeGlobalLiveSets()
            return analysis.liveness
        }
    }
}