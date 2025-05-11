package ir.pass.analysis.dominance

import ir.module.MutationMarker
import ir.module.block.Block
import ir.module.block.Label
import ir.pass.common.AnalysisResult


sealed class AnyDominatorTree(private val head: DomTreeEntry, protected val bbToEntry: Map<Block, DomTreeEntry>, marker: MutationMarker): AnalysisResult(marker) {
    final override fun toString(): String = buildString {
        for ((bb, idom) in bbToEntry) {
            append("BB: '$bb' IDom: '$idom'\n")
        }
    }

    /**
     * Traverse the non-strict dominators of the target block.
     */
    fun traverseDominators(target: Label): Iterator<Block> = object : Iterator<Block> {
        private var current: DomTreeEntry? = bbToEntry[target]!!
        override fun hasNext(): Boolean {
            return current != null
        }

        override fun next(): Block {
            val result = current!!.bb
            current = current!!.iDom
            return result
        }
    }

    /**
     * Simple DFS traversal of dominator tree.
     */
    operator fun iterator(): Iterator<DomTreeEntry> = object : Iterator<DomTreeEntry> {
        private val stack = ArrayDeque<DomTreeEntry>(16) //TODO: magic constant
        private var current: DomTreeEntry? = head

        override fun hasNext(): Boolean {
            return stack.isNotEmpty() || current != null
        }

        override fun next(): DomTreeEntry {
            if (current == null) {
                current = stack.removeLast()
            }
            val result = current!!
            current = null
            for (child in result.dominates) {
                stack.add(child)
            }
            return result
        }
    }
}