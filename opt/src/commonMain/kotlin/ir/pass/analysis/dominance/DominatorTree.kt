package ir.pass.analysis.dominance

import ir.module.MutationMarker
import ir.module.block.Label
import ir.module.block.Block
import ir.pass.common.AnalysisResult


class DominatorTree internal constructor(private val idomMap: Map<Block, Block>, marker: MutationMarker): AnalysisResult(marker) {
    private val cachedDominators = hashMapOf<Label, List<Label>>()

    override fun toString(): String = buildString {
        for ((bb, idom) in idomMap) {
            append("BB: '$bb' IDom: '$idom'\n")
        }
    }

    private fun calculateDominators(target: Label): List<Label> {
        val dom = arrayListOf<Label>()
        var current: Label? = target
        while (current != null) {
            dom.add(current)
            current = idomMap[current]
        }

        return dom
    }

    fun dominates(dominator: Label, target: Label): Boolean {
        var current: Label? = target

        while (current != null) {
            if (current == dominator) {
                return true
            }

            current = idomMap[current]
        }

        return false
    }

    fun dominators(target: Label): List<Label> {
        val saved = cachedDominators[target]
        if (saved != null) {
            return saved
        }

        val dom = calculateDominators(target)
        cachedDominators[target] = dom
        return dom
    }

    fun frontiers(): Map<Block, List<Block>> {
        val dominanceFrontiers = hashMapOf<Block, MutableList<Block>>()

        for (bb in idomMap.keys) {
            dominanceFrontiers[bb] = arrayListOf()
        }

        idomMap.forEach { (bb, idom) ->
            val predecessors = bb.predecessors()
            if (predecessors.size < 2) {
                return@forEach
            }

            for (p in predecessors) {
                var runner: Block = p
                while (runner != idom) {
                    dominanceFrontiers[runner]!!.add(bb)
                    val runnerIdom = idomMap[runner] ?: throw NoSuchElementException("No idom for '$runner'")
                    runner = runnerIdom
                }
            }
        }

        return dominanceFrontiers
    }

    operator fun iterator(): Iterator<Map.Entry<Block, Block>> {
        return idomMap.iterator()
    }
}