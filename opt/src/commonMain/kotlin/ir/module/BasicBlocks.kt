package ir.module

import common.assertion
import ir.instruction.Return
import ir.module.block.Block
import ir.module.block.Label
import ir.module.block.LabelResolver


class BasicBlocks private constructor(private var maxBBIndex: Int, private val basicBlocks: MutableList<Block>): LabelResolver, Iterable<Block> {
    fun blocks(): List<Block> = basicBlocks

    fun size(): Int = basicBlocks.size

    override fun findBlock(label: Label): Block {
        if (label is Block) {
            assertion(basicBlocks.contains(label)) { "Cannot find correspond block: $label" }
            return label
        }
        return basicBlocks.find { it.index == label.index }
            ?: throw IllegalArgumentException("Cannot find correspond block: $label")
    }

    fun begin(): Block {
        assertion(basicBlocks.isNotEmpty() && basicBlocks.first().index == Label.entry.index) {
            "First block should be entry block, but got '${basicBlocks.first()}'"
        }
        return basicBlocks[0]
    }

    fun end(): Block {
        val endBlock = basicBlocks.last()
        assertion(endBlock.lastOrNull() is Return) {
            "Last instruction should be return, but got '${endBlock.last()}'"
        }
        return endBlock
    }

    internal fun swapBlocks(aIndex: Int, bIndex: Int) {
        val a = basicBlocks[aIndex]
        val b = basicBlocks[bIndex]
        basicBlocks[aIndex] = b
        basicBlocks[bIndex] = a
    }

    fun createBlock(): Block {
        val index = maxBBIndex
        maxBBIndex += 1
        val block = Block.empty(index)
        basicBlocks.add(block)
        return block
    }

    override operator fun iterator(): Iterator<Block> {
        return basicBlocks.iterator()
    }

    companion object {
        fun create(): BasicBlocks {
            val startBB = Block.empty(Label.entry.index)
            return BasicBlocks(1, arrayListOf(startBB))
        }
    }
}
