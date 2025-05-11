package ir.pass.analysis.dominance

import ir.module.MutationMarker
import ir.module.block.Label
import ir.module.block.Block
import kotlin.collections.iterator


class PostDominatorTree internal constructor(head: DomTreeEntry, bbToEntry: Map<Block, DomTreeEntry>, marker: MutationMarker): AnyDominatorTree(head, bbToEntry, marker) {
    fun postDominates(postDominator: Label, target: Label): Boolean {
        for (entry in traverseDominators(target)) {
            if (entry == postDominator) {
                return true
            }
        }

        return false
    }
}