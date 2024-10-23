package ir.pass.analysis.traverse.iterator

import ir.instruction.Return
import ir.module.block.Block
import ir.pass.analysis.LoopInfo
import kotlin.collections.first


internal class LinearScanOrderIterator(start: Block, countOfBlocks: Int, private val loopInfo: LoopInfo) : DfsTraversalIterator(countOfBlocks) {
    override var iterator: MutableIterator<Block>

    init {
        foreachLabel(start) { bb -> order.add(bb) }
        iterator = order.iterator()
    }

    private fun foreachLabel(start: Block, callback: Callback) {
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
            if (visited[bb.index]) {
                continue
            }
            callback(bb)
            visited[bb.index] = true

            val successors = bb.successors()
            for (idx in successors.indices.reversed()) {
                stack.add(successors[idx])
            }

            // Special case for loops
            val loopInfo = loopInfo[bb]
            if (loopInfo != null) {
                // Add loop exit and loop enter blocks to the stack
                // Order of adding isn't important
                stack.add(loopInfo.first().exit())
                stack.add(loopInfo.first().enter())
            }
        }

        callback(exitBlock as Block)
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Block {
        return iterator.next()
    }
}