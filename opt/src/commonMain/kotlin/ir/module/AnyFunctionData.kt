package ir.module

import ir.module.block.Block

abstract class AnyFunctionData(protected val blocks: BasicBlocks) {
    fun size(): Int {
        return blocks.size()
    }

    fun begin(): Block {
        return blocks.begin()
    }

    fun end(): Block {
        return blocks.end()
    }

    fun marker(): MutationMarker = blocks.marker()
}