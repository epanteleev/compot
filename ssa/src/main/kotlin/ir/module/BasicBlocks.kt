package ir.module

import ir.DominatorTree
import ir.module.auxiliary.Copy
import ir.module.block.Block
import ir.module.block.Label
import ir.module.block.iterator.BasicBlocksIterator
import ir.module.block.iterator.BfsTraversalIterator
import ir.module.block.iterator.PostorderIterator
import ir.module.block.iterator.PreorderIterator
import ir.utils.DefUseInfo


class BasicBlocks(private val basicBlocks: MutableList<Block>) {
    fun blocks(): MutableList<Block> {
        return basicBlocks
    }

    fun size(): Int {
        return basicBlocks.size
    }

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

    fun preorder(): BasicBlocksIterator {
        return PreorderIterator(begin(), blocks().size)
    }

    fun postorder(): BasicBlocksIterator {
        return PostorderIterator(begin(), blocks().size)
    }

    fun bfsTraversal(): BasicBlocksIterator {
        return BfsTraversalIterator(begin(), blocks().size)
    }

    fun linearScanOrder(): BasicBlocksIterator {
        return bfsTraversal()
    }

    fun dominatorTree(): DominatorTree {
        return DominatorTree.evaluate(this)
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
        return Copy.copy(this)
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
