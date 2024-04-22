package ir.platform.x64.regalloc.liveness

import ir.LocalValue
import ir.module.FunctionData
import ir.module.block.Block


data class LiveInfo(val liveIn: Set<LocalValue>, val gen: Set<LocalValue>, val liveOut: Set<LocalValue>)

class NewLivenessAnalysis private constructor(val data: FunctionData) {
    private val liveness = hashMapOf<Block, LiveInfo>()
    private val loopInfo = data.blocks.loopInfo()
    private val linearScanOrder = data.blocks.linearScanOrder(loopInfo).order()

    private fun computeLocalLiveSets() {
        for (bb in linearScanOrder) {
            val liveIn = mutableSetOf<LocalValue>()
            val liveOut = mutableSetOf<LocalValue>()
            val gen = mutableSetOf<LocalValue>()
            val kill = mutableSetOf<LocalValue>()

            for (inst in bb.instructions()) {
                // Handle input operands
                for (usage in inst.operands()) {
                    if (usage !is LocalValue) {
                        continue
                    }

                    if (usage !in kill) {
                        gen.add(usage)
                    }
                }

                // Handle output operand
                if (inst is LocalValue) {
                    kill.add(inst)
                }

                liveIn.addAll(gen)
                liveIn.removeAll(kill)
                liveOut.addAll(liveIn)
                liveOut.removeAll(gen)
            }

            liveness[bb] = LiveInfo(liveIn, gen, liveOut)
        }
    }

    private fun computeGlobalLiveSets() {
        computeLocalLiveSets()
        do {
            var changed = false
            for (bb in linearScanOrder.reversed()) {
                val liveIn = mutableSetOf<LocalValue>()
                val liveOut = mutableSetOf<LocalValue>()
                val gen = mutableSetOf<LocalValue>()

                for (succ in bb.successors()) {
                    liveOut.addAll(liveness[succ]!!.liveIn)
                }

                val currentLiveIn = liveness[bb]!!.liveIn
                val currentLiveOut = liveness[bb]!!.liveOut

                if (currentLiveIn != liveIn || currentLiveOut != liveOut) {
                    liveness[bb] = LiveInfo(liveIn, gen, liveOut)
                    changed = true
                }
            }
        } while (changed)
    }


    companion object {
        fun evaluate(data: FunctionData): Map<Block, LiveInfo> {
            return NewLivenessAnalysis(data).apply {
                computeGlobalLiveSets()
            }.liveness
        }
    }
}