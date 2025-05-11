package ir.pass.analysis.dominance

import ir.module.MutationMarker
import ir.module.block.Label
import ir.module.block.Block

class DominatorTree internal constructor(head: DomTreeEntry, bbToEntry: Map<Block, DomTreeEntry>, marker: MutationMarker): AnyDominatorTree(head,bbToEntry, marker) {
    fun dominates(dominator: Label, target: Label): Boolean {
        for (entry in traverseDominators(target)) {
            if (entry == dominator) return true
        }

        return false
    }

    fun dominators(target: Block): Iterator<Block> = traverseDominators(target)

    fun frontiers(): Map<Block, List<Block>> { //TODO: improve algorithm?
        val dominanceFrontiers = hashMapOf<Block, MutableList<Block>>()

        for ((bb, domEntry) in bbToEntry) {
            if (domEntry.iDom == null) {
                continue
            }

            dominanceFrontiers[bb] = arrayListOf()
        }

        bbToEntry.forEach { (bb, idom) ->
            val predecessors = bb.predecessors()
            if (predecessors.size < 2) {
                return@forEach
            }

            for (p in predecessors) {
                var runner: Block = p
                while (runner != idom.iDom?.bb) {
                    dominanceFrontiers[runner]!!.add(bb)
                    val runnerIdom = bbToEntry[runner] ?: throw NoSuchElementException("No idom for '$runner'")
                    runner = runnerIdom.iDom?.bb ?: break
                }
            }
        }

        return dominanceFrontiers
    }
}