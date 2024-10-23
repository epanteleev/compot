package ir.pass.analysis.traverse.iterator

import ir.module.block.Block

internal class BackwardPostorderIterator(exit: Block, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    override var iterator: MutableIterator<Block>
    init {
        backwardDfsForeachLabel(exit) { bb -> order.add(bb) }
        order.reverse()
        iterator = order.iterator()
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Block {
        return iterator.next()
    }
}