package ir.pass.analysis.dominance

import ir.module.MutationMarker
import ir.module.block.Label
import ir.module.block.AnyBlock
import ir.pass.common.AnalysisResult
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator


class PostDominatorTree internal constructor(private val ipdomMap: Map<AnyBlock, AnyBlock>, marker: MutationMarker): AnalysisResult(marker) {
    private val cachedPostDominators = hashMapOf<Label, List<Label>>()

    override fun toString(): String = buildString {
        for ((bb, ipdom) in ipdomMap) {
            append("BB: '$bb' IpDom: '$ipdom'\n")
        }
    }

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
        val saved = cachedPostDominators[target]
        if (saved != null) {
            return saved
        }

        val dom = calculatePostDominators(target)
        cachedPostDominators[target] = dom
        return dom
    }


    operator fun iterator(): Iterator<Map.Entry<AnyBlock, AnyBlock>> {
        return ipdomMap.iterator()
    }
}