package ir.module.block.iterator

import ir.module.block.Block


class BfsTraversalIterator(start: Block, countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    private val iterator: MutableIterator<Block>

    init {
        bfsForeachLabel(start) { bb -> order.add(bb) }
        iterator = order.iterator()
    }

    private fun bfsForeachLabel(start: Block, callback: Callback) {
        val stack = arrayListOf<List<Block>>()
        val visited = BooleanArray(countOfBlocks)
        fun visitBlock(bb: Block) {
            callback(bb)
            visited[bb.index] = true
            if (bb.successors().isNotEmpty()) {
                stack.add(bb.successors())
            }
        }

        visitBlock(start)

        while (stack.isNotEmpty()) {
            val basicBlocks = stack.removeLast()

            for (bb in basicBlocks) {
                if (visited[bb.index]) {
                    continue
                }
                visitBlock(bb)
            }
        }
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Block {
        return iterator.next()
    }
}