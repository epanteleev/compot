package ir.pass.analysis.traverse.iterator

import ir.instruction.Return
import ir.module.block.Block
import ir.module.block.Label


sealed class DfsTraversalIterator(countOfBlocks: Int) : BasicBlocksIterator(countOfBlocks) {
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
            if (visited[bb.index]) {
                continue
            }

            callback(bb)
            visited[bb.index] = true

            val successors = bb.successors()
            for (idx in successors.indices.reversed()) {
                stack.add(successors[idx])
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
            if (visited[bb.index]) {
                continue
            }

            callback(bb)
            visited[bb.index] = true

            val predecessors = bb.predecessors()
            for (idx in predecessors.indices.reversed()) {
                stack.add(predecessors[idx])
            }
        }

        callback(startBlock as Block)
    }
}