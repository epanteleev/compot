package ir.dominance

import ir.module.BasicBlocks
import ir.module.block.Label
import ir.module.block.AnyBlock


class PostDominatorTree(private val ipdomMap: Map<AnyBlock, AnyBlock>) {
    private val cachedDominators = hashMapOf<Label, List<Label>>()

    private fun calculatePostDominators(target: Label): List<Label> {
        val dom = arrayListOf<Label>()
        var current: Label? = target
        while (current != null) {
            dom.add(current)
            current = ipdomMap[current]
        }

        return dom
    }

    fun postDominates(postDominator: Label, target: Label): Boolean {
        var current: Label? = target

        while (current != null) {
            if (current == postDominator) {
                return true
            }

            current = ipdomMap[current]
        }

        return false
    }

    fun postDominators(target: Label): List<Label> {
        val saved = cachedDominators[target]
        if (saved != null) {
            return saved
        }

        val dom = calculatePostDominators(target)
        cachedDominators[target] = dom
        return dom
    }


    operator fun iterator(): Iterator<Map.Entry<AnyBlock, AnyBlock>> {
        return ipdomMap.iterator()
    }

    companion object {
        fun evaluate(basicBlocks: BasicBlocks): PostDominatorTree {
            return PostDominatorTreeCalculate.evaluate(basicBlocks)
        }
    }
}