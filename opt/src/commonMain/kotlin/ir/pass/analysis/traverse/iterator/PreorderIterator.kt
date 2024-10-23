package ir.pass.analysis.traverse.iterator

import ir.module.block.Block

internal class PreorderIterator(start: Block, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    override var iterator: MutableIterator<Block>

    init {
        dfsForeachLabel(start) { bb -> order.add(bb) }
        iterator = order.iterator()
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Block {
        return iterator.next()
    }
}