package ir.iterator

import ir.BasicBlock
import ir.Return
import java.util.*

typealias Callback = (BasicBlock) -> Unit

abstract class BasicBlocksIterator(countOfBlocks: Int): Iterator<BasicBlock> {
    private val visited = BooleanArray(countOfBlocks)
    private val stack = LinkedList<BasicBlock>()

    protected fun foreachLabel(start: BasicBlock, callback: Callback) {
        stack.push(start)

        var exutBlock: BasicBlock? = null
        while (!stack.isEmpty()) {
            val bb = stack.pop()!!

            if (bb.flowInstruction() is Return) {
                exutBlock = bb
                continue
            }
            if (!visited[bb.index()]) {
                callback(bb)
                visited[bb.index()] = true
                stack.addAll(bb.successors)
            }
        }

        callback(exutBlock as BasicBlock)
    }
}

class PostorderIterator(start: BasicBlock, countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    private val postorder = LinkedList<BasicBlock>()
    private val iterator: MutableIterator<BasicBlock>

    init {
        foreachLabel(start) { bb -> postorder.push(bb) }
        iterator = postorder.iterator()
    }
    
    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): BasicBlock {
        return iterator.next()
    }
}

class PreorderIterator(start: BasicBlock, countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
    private val preorder = LinkedList<BasicBlock>()
    private val iterator: MutableIterator<BasicBlock>

    init {
        foreachLabel(start) { bb -> preorder.add(bb) }
        iterator = preorder.iterator()
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): BasicBlock {
        return iterator.next()
    }
}