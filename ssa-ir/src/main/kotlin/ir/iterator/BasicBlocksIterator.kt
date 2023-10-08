package ir.iterator

import ir.block.Block
import ir.instruction.Return
import java.util.*

typealias Callback = (Block) -> Unit

abstract class BasicBlocksIterator(countOfBlocks: Int): Iterator<Block> {
    protected val order = ArrayList<Block>(countOfBlocks)
    protected val visited = BooleanArray(countOfBlocks)

    fun order(): List<Block> {
        return order
    }
}

abstract class DfsTraversalIterator(countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    protected abstract var iterator: MutableIterator<Block>

    protected fun dfsForeachLabel(start: Block, callback: Callback) {
        val stack = arrayListOf<Block>()
        stack.add(start)

        var exitBlock: Block? = null
        while (stack.isNotEmpty()) {
            val bb = stack.removeLast()

            if (bb.last() is Return) {
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