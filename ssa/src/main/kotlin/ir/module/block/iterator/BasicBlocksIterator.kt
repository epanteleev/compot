package ir.module.block.iterator

import ir.instruction.Return
import ir.module.block.Block
import ir.module.block.Label


typealias Callback = (Block) -> Unit

abstract class BasicBlocksIterator(protected val countOfBlocks: Int): Iterator<Block> {
    protected val order = ArrayList<Block>(countOfBlocks)
    fun order(): List<Block> = order
}

abstract class DfsTraversalIterator(countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    protected abstract var iterator: MutableIterator<Block>

    protected fun dfsForeachLabel(start: Block, callback: Callback) {
        val visited = BooleanArray(countOfBlocks)
        val stack = arrayListOf<Block>()
        stack.add(start)

        var exitBlock: Block? = null
        while (stack.isNotEmpty()) {
            val bb = stack.removeLast()

            val last = bb.last()
            if (last is Return) {
                exitBlock = bb
                continue
            }
            if (!visited[bb.index]) {
                callback(bb)
                visited[bb.index] = true

                val successors = bb.successors()
                for (idx in successors.indices.reversed()) {
                    stack.add(successors[idx])
                }
            }
        }

        callback(exitBlock as Block)
    }

    protected fun backwardDfsForeachLabel(exit: Block, callback: Callback) {
        val visited = BooleanArray(countOfBlocks)
        val stack = arrayListOf<Block>()
        stack.add(exit)

        var startBlock: Block? = null
        while (stack.isNotEmpty()) {
            val bb = stack.removeLast()

            if (bb.equals(Label.entry)) {
                startBlock = bb
                continue
            }
            if (!visited[bb.index]) {
                callback(bb)
                visited[bb.index] = true

                val predecessors = bb.predecessors()
                for (idx in predecessors.indices.reversed()) {
                    stack.add(predecessors[idx])
                }
            }
        }

        callback(startBlock as Block)
    }
}

class PostorderIterator(start: Block, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    override var iterator: MutableIterator<Block>
    init {
        dfsForeachLabel(start) { bb -> order.add(bb) }
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

class BackwardPostorderIterator(exit: Block, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
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

class PreorderIterator(start: Block, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
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