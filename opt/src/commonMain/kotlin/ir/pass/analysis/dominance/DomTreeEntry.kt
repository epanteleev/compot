package ir.pass.analysis.dominance

import ir.module.block.Block

sealed class DomTreeEntry(internal var iDom: DomTreeEntry?, val bb: Block, internal val dominates: MutableSet<DomTreeEntry>) {
    override fun toString(): String {
        return "DomEntry(idom=${iDom?.bb}, dominates=${dominates.map { it.bb }})"
    }

    fun idom(): Block? {
        return iDom?.bb
    }

    fun dominates(): Iterator<Block> = object : Iterator<Block> {
        private val iterator = dominates.iterator()
        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override fun next(): Block {
            return iterator.next().bb
        }
    }
}

internal class DomTreeEntryImpl(iDom: DomTreeEntry?, bb: Block, dominates: MutableSet<DomTreeEntry>) : DomTreeEntry(iDom, bb, dominates) {
    override fun toString(): String {
        return "DomEntryImpl(idom=${idom()}, dominates=${dominates.map { it.bb }})"
    }
}