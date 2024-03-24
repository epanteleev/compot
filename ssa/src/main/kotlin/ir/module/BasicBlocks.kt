package ir.module

import ir.dominance.DominatorTree
import ir.dominance.PostDominatorTree
import ir.instruction.Return
import ir.module.auxiliary.CopyCFG
import ir.module.block.Block
import ir.module.block.Label
import ir.module.block.iterator.*
import ir.pass.ana.LoopDetection
import ir.pass.ana.LoopInfo
import ir.utils.DefUseInfo


class BasicBlocks(private val basicBlocks: MutableList<Block>) {
    fun blocks(): MutableList<Block> = basicBlocks

    fun size(): Int = basicBlocks.size

    fun findBlock(label: Label): Block {
        return basicBlocks.find { it.index == label.index }
            ?: throw IllegalArgumentException("Cannot find correspond block: $label")
    }

    fun maxBlockIndex(): Int {
        return basicBlocks.maxBy { it.index }.index
    }

    fun begin(): Block {
        return basicBlocks[0]
    }

    fun end(): Block {
        val endBlock = basicBlocks.find { it.last() is Return }
        assert(endBlock != null) { "graph doesn't have enr block" }
        return endBlock as Block
    }

    fun preorder(): BasicBlocksIterator {
        return PreorderIterator(begin(), size())
    }

    fun postorder(): BasicBlocksIterator {
        return PostorderIterator(begin(), size())
    }

    fun backwardPostorder(): BasicBlocksIterator {
        return BackwardPostorderIterator(end(), size())
    }

    fun bfsTraversal(): BasicBlocksIterator {
        return BfsTraversalIterator(begin(), size())
    }

    fun linearScanOrder(loopInfo: LoopInfo): BasicBlocksIterator {
        return LinearScanOrderIterator(begin(), size(), loopInfo)
    }

    fun loopInfo(): LoopInfo {
        return LoopDetection.evaluate(this)
    }

    fun dominatorTree(): DominatorTree {
        return DominatorTree.evaluate(this)
    }

    fun postDominatorTree(): PostDominatorTree {
        return PostDominatorTree.evaluate(this)
    }

    fun putBlock(block: Block) {
        basicBlocks.add(block)
    }

    fun defUseInfo(): DefUseInfo {
        return DefUseInfo.create(this)
    }

    operator fun iterator(): Iterator<Block> {
        return basicBlocks.iterator()
    }

    fun copy(): BasicBlocks {
        return CopyCFG.copy(this)
    }

    companion object {
        fun create(startBB: Block): BasicBlocks {
            return BasicBlocks(arrayListOf(startBB))
        }

        fun create(blocks: MutableList<Block>): BasicBlocks {
            return BasicBlocks(blocks)
        }
    }
}
