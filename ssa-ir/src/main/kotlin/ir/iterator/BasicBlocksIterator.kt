package ir.iterator

import ir.*
import java.util.*

typealias Callback = (BasicBlock) -> Unit

abstract class BasicBlocksIterator(countOfBlocks: Int): Iterator<BasicBlock> {
    protected val order = ArrayList<BasicBlock>(countOfBlocks)
    protected val visited = BooleanArray(countOfBlocks)

    fun order(): List<BasicBlock> {
        return order
    }
}

abstract class DfsTraversalIterator(countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    protected abstract var iterator: MutableIterator<BasicBlock>

    protected fun dfsForeachLabel(start: BasicBlock, callback: Callback) {
        val stack = arrayListOf<BasicBlock>()
        stack.add(start)

        var exitBlock: BasicBlock? = null
        while (stack.isNotEmpty()) {
            val bb = stack.removeLast()

            if (bb.flowInstruction() is Return) {
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

        callback(exitBlock as BasicBlock)
    }
}

class PostorderIterator(start: BasicBlock, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    override var iterator: MutableIterator<BasicBlock>
    init {
        dfsForeachLabel(start) { bb -> order.add(bb) }
        order.reverse()
        iterator = order.iterator()
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): BasicBlock {
        return iterator.next()
    }
}

class PreorderIterator(start: BasicBlock, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    override var iterator: MutableIterator<BasicBlock>

    init {
        dfsForeachLabel(start) { bb -> order.add(bb) }
        iterator = order.iterator()
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): BasicBlock {
        return iterator.next()
    }
}

class BfsTraversalIterator(start: BasicBlock, countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    private val iterator: MutableIterator<BasicBlock>

    init {
        bfsForeachLabel(start) { bb -> order.add(bb) }
        iterator = order.iterator()
    }

    private fun bfsForeachLabel(start: BasicBlock, callback: Callback) {
        val stack = arrayListOf<List<BasicBlock>>()
        fun visitBlock(bb: BasicBlock) {
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

    override fun next(): BasicBlock {
        return iterator.next()
    }
}