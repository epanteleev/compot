package ir

import ir.block.Block
import ir.block.Label
import ir.iterator.*
import ir.utils.DefUseInfo
import kotlin.math.max


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

    fun copy(oldArgumentValues: List<ArgumentValue>, newArgumentValues: List<ArgumentValue>) {
        fun setupValuesMap(): MutableMap<LocalValue, LocalValue> {
            val oldValuesToOld = hashMapOf<LocalValue, LocalValue>()
            for ((old, new) in oldArgumentValues zip newArgumentValues) {
                oldValuesToOld[old] = new
            }

            return oldValuesToOld
        }

        fun setupNewBasicBlock(): Map<Block, Block> {
            val newBasicBlocks = basicBlocks.mapTo(arrayListOf()) {
                Block.empty(it.index)
            }

            val oldToNew = hashMapOf<Block, Block>()
            for ((old, new) in basicBlocks zip newBasicBlocks) {
                oldToNew[old] = new
            }

            return oldToNew
        }

        val oldToNew       = setupNewBasicBlock()
        val oldValuesToOld = setupValuesMap()

        for (bb in basicBlocks) {
            val newBB = oldToNew[bb]!!
            for (inst in bb.instructions()) {
                when (inst) {
//                    is TrivialCopybable -> {
//                        newBB.copy { i ->
//                            val newUsages = inst.usages().mapTo(arrayListOf()) { oldValuesToOld[it]!! }
//                            inst.copy(newUsages)
//                        }
//                    }
                }
            }
        }
    }

    operator fun iterator(): Iterator<Block> {
        return basicBlocks.iterator()
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
