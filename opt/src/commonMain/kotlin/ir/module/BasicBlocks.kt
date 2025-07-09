package ir.module

import common.assertion
import ir.instruction.Return
import ir.module.block.Block
import ir.module.block.Label


class BasicBlocks private constructor(): LabelResolver, AnyBasicBlocks<Block>() {
    private val basicBlocks = arrayListOf(Block.empty(modificationCounter, Label.entry.index))
    private var maxBBIndex: Int = 1

    internal fun blocks(): List<Block> = basicBlocks

    override fun size(): Int = basicBlocks.size

    override fun findBlock(label: Label): Block {
        if (label is Block) {
            assertion(basicBlocks.contains(label)) { "Cannot find correspond block: $label" }
            return label
        }
        return basicBlocks.find { it.index == label.index }
            ?: throw IllegalArgumentException("Cannot find correspond block: $label")
    }

   override fun begin(): Block {
        assertion(basicBlocks.firstOrNull() == Label.entry) {
            "First block should be entry block, but got '${basicBlocks.firstOrNull()}'"
        }

        return basicBlocks[0]
    }

    override fun end(): Block {
        return basicBlocks.find { it.lastOrNull() is Return }
            ?: throw IllegalStateException("Function data must have a return block")
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
