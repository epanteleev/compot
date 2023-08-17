package ir.iterator

import ir.BasicBlock
import ir.Return
import java.util.*

typealias Callback = (BasicBlock) -> Unit

abstract class BasicBlocksIterator(countOfBlocks: Int): Iterator<BasicBlock> {
    protected val visited = BooleanArray(countOfBlocks)
}

abstract class DfsTraversalIterator(countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    protected val order = ArrayList<BasicBlock>(countOfBlocks)
    protected var iterator: MutableIterator<BasicBlock> = order.iterator()

    protected fun dfsForeachLabel(start: BasicBlock, callback: Callback) {
        val stack = LinkedList<BasicBlock>()
        stack.push(start)

        var exitBlock: BasicBlock? = null
        while (!stack.isEmpty()) {
            val bb = stack.pop()!!

            if (bb.flowInstruction() is Return) {
                exitBlock = bb
                continue
            }
            if (!visited[bb.index()]) {
                callback(bb)
                visited[bb.index()] = true
                stack.addAll(bb.successors)
            }
        }

        callback(exitBlock as BasicBlock)
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): BasicBlock {
        return iterator.next()
    }
}

class PostorderIterator(start: BasicBlock, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    init {
        dfsForeachLabel(start) { bb -> order.add(bb) }
        order.reverse()
        iterator = order.iterator()
    }
}

class PreorderIterator(start: BasicBlock, countOfBlocks: Int) : DfsTraversalIterator(countOfBlocks) {
    init {
        dfsForeachLabel(start) { bb -> order.add(bb) }
        iterator = order.iterator()
    }
}

class BfsTraversalIterator(start: BasicBlock, countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    private val bfsOrdered = ArrayList<BasicBlock>(countOfBlocks)
    private val iterator: MutableIterator<BasicBlock>

    init {
        bfsForeachLabel(start) { bb -> bfsOrdered.add(bb) }
        iterator = bfsOrdered.iterator()
    }

    private fun bfsForeachLabel(start: BasicBlock, callback: Callback) {
        val stack = LinkedList<List<BasicBlock>>()
        fun visitBlock(bb: BasicBlock) {
            callback(bb)
            visited[bb.index()] = true
            if (bb.successors.isNotEmpty()) {
                stack.add(bb.successors)
            }
        }

        visitBlock(start)

        while (!stack.isEmpty()) {
            val basicBlocks = stack.pop()!!
            for (bb in basicBlocks) {
                if (visited[bb.index()]) {
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