package ir.dominance

import ir.module.FunctionData
import ir.module.block.Label
import ir.module.block.AnyBlock


class DominatorTree(private val idomMap: Map<AnyBlock, AnyBlock>) {
    private val cachedDominators = hashMapOf<Label, List<Label>>()

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

    fun frontiers(): Map<AnyBlock, List<AnyBlock>> {
        val dominanceFrontiers = hashMapOf<AnyBlock, MutableList<AnyBlock>>()

        for (bb in idomMap.keys) {
            dominanceFrontiers[bb] = arrayListOf()
        }

        idomMap.forEach { (bb, idom) ->
            val predecessors = bb.predecessors()
            if (predecessors.size < 2) {
                return@forEach
            }

            for (p in predecessors) {
                var runner: AnyBlock = p
                while (runner != idom) {
                    dominanceFrontiers[runner]!!.add(bb)
                    val runnerIdom = idomMap[runner] ?: throw NoSuchElementException("No idom for '$runner'")
                    runner = runnerIdom
                }
            }
        }

        return dominanceFrontiers
    }

    operator fun iterator(): Iterator<Map.Entry<AnyBlock, AnyBlock>> {
        return idomMap.iterator()
    }

    companion object {
        fun evaluate(basicBlocks: FunctionData): DominatorTree {
            return DominatorTreeCalculate.evaluate(basicBlocks)
        }
    }
}