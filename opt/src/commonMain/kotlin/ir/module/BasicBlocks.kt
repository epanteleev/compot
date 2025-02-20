package ir.module

import common.assertion
import ir.instruction.Return
import ir.module.block.Block
import ir.module.block.Label


class BasicBlocks private constructor(): LabelResolver, Iterable<Block> {
    private val modificationCounter = ModificationCounter()
    private val basicBlocks = arrayListOf(Block.empty(modificationCounter, Label.entry.index))
    private var maxBBIndex: Int = 1

    internal fun marker(): MutationMarker = modificationCounter.mutations()

    internal fun blocks(): List<Block> = basicBlocks

    fun size(): Int = basicBlocks.size

    override fun findBlock(label: Label): Block {
        if (label is Block) {
            assertion(basicBlocks.contains(label)) { "Cannot find correspond block: $label" }
            return label
        }
        return basicBlocks.find { it.index == label.index }
            ?: throw IllegalArgumentException("Cannot find correspond block: $label")
    }

    internal fun begin(): Block {
        assertion(basicBlocks.isNotEmpty() && basicBlocks.first() == Label.entry) {
            "First block should be entry block, but got '${basicBlocks.first()}'"
        }
        return basicBlocks[0]
    }

    internal fun end(): Block {
        val endBlock = basicBlocks.last()
        assertion(endBlock.lastOrNull() is Return) {
            "Last instruction should be return, but got '${endBlock.lastOrNull()}'"
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
        val block = Block.empty(modificationCounter, index)
        basicBlocks.add(block)
        return block
    }

    override operator fun iterator(): Iterator<Block> {
        return basicBlocks.iterator()
    }

    companion object {
        fun create(): BasicBlocks {
            return BasicBlocks()
        }
    }
}
