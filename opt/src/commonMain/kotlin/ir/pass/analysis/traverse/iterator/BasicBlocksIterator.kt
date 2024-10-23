package ir.pass.analysis.traverse.iterator

import ir.module.block.Block


typealias Callback = (Block) -> Unit

sealed class BasicBlocksIterator(protected val countOfBlocks: Int): Iterator<Block> {
    protected val order = ArrayList<Block>(countOfBlocks)
    fun order(): List<Block> = order
}