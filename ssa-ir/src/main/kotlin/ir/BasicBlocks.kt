package ir

import ir.iterator.*
import ir.utils.DefUseInfo
import kotlin.math.max


class BasicBlocks(private val basicBlocks: MutableList<BasicBlock>) {
    fun blocks(): MutableList<BasicBlock> {
        return basicBlocks
    }

    fun size(): Int {
        return basicBlocks.size
    }

    fun findBlock(label: Label): BasicBlock {
        return basicBlocks.find { it.index == label.index }
            ?: throw IllegalArgumentException("Cannot find correspond block: $label")
    }

    fun maxBlockIndex(): Int {
        return basicBlocks.maxBy { it.index }.index
    }

    fun maxInstructionIndex(): Int {
        var index = -1
        for (bb in basicBlocks) {
            for (inst in bb) {
                if (inst !is ValueInstruction) {
                    continue
                }

                index = max(index, inst.defined())
            }
        }
        return index
    }

    fun begin(): BasicBlock {
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

    fun putBlock(block: BasicBlock) {
        basicBlocks.add(block)
    }

    fun defUseInfo(): DefUseInfo {
        return DefUseInfo.create(this)
    }

    operator fun iterator(): Iterator<BasicBlock> {
        return basicBlocks.iterator()
    }

    companion object {
        fun create(startBB: BasicBlock): BasicBlocks {
            return BasicBlocks(arrayListOf(startBB))
        }

        fun create(blocks: MutableList<BasicBlock>): BasicBlocks {
            return BasicBlocks(blocks)
        }
    }
}
